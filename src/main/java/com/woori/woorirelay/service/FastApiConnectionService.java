/**
 *
 *
 * <pre>
 * <b>Description  : FastAPI Gateway 아웃바운드 WebSocket 연결</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.service
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.06.22        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayMetrics;
import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.TerminationReason;
import com.woori.woorirelay.handler.FastApiBackendHandler;
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.constant.RelayConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class FastApiConnectionService {

    private final RelayProperties relayProperties;
    private final StandardWebSocketClient webSocketClient;
    private final VoiceSessionLifecycleService lifecycleService;
    private final RelayMetrics relayMetrics;
    private final FastApiCircuitBreaker circuitBreaker;

    public void connect(
            VoiceSessionEntry entry,
            Consumer<String> resultHandler,
            Runnable disconnectHandler
    ) {
        String backendUrl = buildBackendUrl(entry);

        // CB 가 OPEN 이면 게이트웨이 지속 장애 → 시도 없이 빠르게 종료(thundering-herd 방지).
        if (!circuitBreaker.allowRequest()) {
            relayMetrics.recordGatewayConnectionFailure();
            log.warn("[FastApiConnection] Circuit OPEN, skipping connect registryKey={} url={}",
                    entry.getRegistryKey(), backendUrl);
            lifecycleService.terminateSession(entry.getRegistryKey(), CloseStatus.SERVER_ERROR,
                    TerminationReason.FASTAPI_CONNECTION_FAILURE, false);
            return;
        }

        RelayProperties.FastApi cfg = relayProperties.getFastApi();
        int maxAttempts = Math.max(1, cfg.getConnectMaxAttempts());
        Exception lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            FastApiBackendHandler backendHandler = new FastApiBackendHandler(
                    entry.getSessionId(),
                    entry.getRegistryKey(),
                    resultHandler,
                    disconnectHandler,
                    relayMetrics
            );
            try {
                WebSocketSession backendSession = webSocketClient
                        .execute(backendHandler, null, URI.create(backendUrl))
                        .get(RelayConstants.FASTAPI_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

                entry.bindBackendSession(backendSession, backendHandler);
                circuitBreaker.onSuccess();
                log.info("[FastApiConnection] Connected registryKey={} direction={} url={} attempt={}",
                        entry.getRegistryKey(), entry.getDirection(), backendUrl, attempt);
                return;
            } catch (Exception ex) {
                lastError = ex;
                log.warn("[FastApiConnection] Attempt {}/{} failed registryKey={} url={}: {}",
                        attempt, maxAttempts, entry.getRegistryKey(), backendUrl, ex.toString());
                if (attempt < maxAttempts && !sleepBackoff(cfg.getConnectBackoffMs() * attempt)) {
                    break; // 인터럽트 시 중단
                }
            }
        }

        circuitBreaker.onFailure();
        relayMetrics.recordGatewayConnectionFailure();
        log.error("[FastApiConnection] Failed after {} attempt(s) registryKey={} url={}",
                maxAttempts, entry.getRegistryKey(), backendUrl, lastError);
        lifecycleService.terminateSession(
                entry.getRegistryKey(),
                CloseStatus.SERVER_ERROR,
                TerminationReason.FASTAPI_CONNECTION_FAILURE,
                false
        );
    }

    private boolean sleepBackoff(long millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public String buildBackendUrl(VoiceSessionEntry entry) {
        return buildBackendUrl(entry.getDirection(), entry.getSessionId());
    }

    public String buildBackendUrl(CallDirection direction, String sessionId) {
        String baseUrl = relayProperties.resolveFastApiWsBaseUrl(direction);
        if (baseUrl.endsWith("/")) {
            return baseUrl + sessionId;
        }
        return baseUrl + "/" + sessionId;
    }
}
