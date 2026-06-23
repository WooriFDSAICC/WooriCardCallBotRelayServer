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

    public void connect(
            VoiceSessionEntry entry,
            Consumer<String> resultHandler,
            Runnable disconnectHandler
    ) {
        String backendUrl = buildBackendUrl(entry);

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
            log.info("[FastApiConnection] Connected registryKey={} direction={} url={}",
                    entry.getRegistryKey(), entry.getDirection(), backendUrl);
        } catch (Exception ex) {
            relayMetrics.recordGatewayConnectionFailure();
            log.error("[FastApiConnection] Failed registryKey={} url={}", entry.getRegistryKey(), backendUrl, ex);
            lifecycleService.terminateSession(
                    entry.getRegistryKey(),
                    CloseStatus.SERVER_ERROR,
                    TerminationReason.FASTAPI_CONNECTION_FAILURE,
                    false
            );
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
