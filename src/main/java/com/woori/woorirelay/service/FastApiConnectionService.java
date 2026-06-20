package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.TerminationReason;
import com.woori.woorirelay.handler.FastApiBackendHandler;
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

    public void connect(
            VoiceSessionEntry entry,
            Consumer<String> resultHandler,
            Runnable disconnectHandler
    ) {
        String sessionId = entry.getSessionId();
        String backendUrl = buildBackendUrl(sessionId);

        FastApiBackendHandler backendHandler = new FastApiBackendHandler(
                sessionId,
                resultHandler,
                disconnectHandler
        );

        try {
            WebSocketSession backendSession = webSocketClient
                    .execute(backendHandler, null, URI.create(backendUrl))
                    .get(RelayConstants.FASTAPI_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);

            entry.bindBackendSession(backendSession);
            log.info("[FastApiConnection] Connected sessionId={} url={}", sessionId, backendUrl);
        } catch (Exception ex) {
            log.error("[FastApiConnection] Failed sessionId={} url={}", sessionId, backendUrl, ex);
            lifecycleService.terminateSession(
                    sessionId,
                    CloseStatus.SERVER_ERROR,
                    TerminationReason.FASTAPI_CONNECTION_FAILURE,
                    false
            );
        }
    }

    public String buildBackendUrl(String sessionId) {
        String baseUrl = relayProperties.getFastApiWsBaseUrl();
        if (baseUrl.endsWith("/")) {
            return baseUrl + sessionId;
        }
        return baseUrl + "/" + sessionId;
    }
}
