package com.woori.woorirelay.handler;

import com.woori.woorirelay.constant.RelayCloseStatus;
import com.woori.woorirelay.constant.WebSocketConstants;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.service.VoicePipelineService;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.support.SessionIdExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

/**
 * 고객/교환기 인입 WebSocket 핸들러 — 커넥션 수립 및 PCM 오디오 패싱 총괄.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceIntermediaryHandler extends BinaryWebSocketHandler {

    private final VoiceSessionRegistry sessionRegistry;
    private final VoicePipelineService pipelineService;
    private final SessionIdExtractor sessionIdExtractor;

    @Override
    public void afterConnectionEstablished(WebSocketSession clientSession) throws Exception {
        String sessionId = sessionIdExtractor.extract(clientSession);
        if (sessionId == null || sessionId.isBlank()) {
            log.warn("[Handler] Missing sessionId, closing wsId={}", clientSession.getId());
            clientSession.close(RelayCloseStatus.BAD_DATA);
            return;
        }

        if (!sessionRegistry.registerIfAbsent(sessionId, clientSession)) {
            clientSession.close(RelayCloseStatus.DUPLICATE_SESSION);
            return;
        }

        clientSession.getAttributes().put(WebSocketConstants.SESSION_ID_ATTRIBUTE, sessionId);

        VoiceSessionEntry entry = sessionRegistry.find(sessionId)
                .orElseThrow(() -> new IllegalStateException("Session entry missing after register"));

        pipelineService.onSessionStarted(sessionId, entry);
        log.info("[Handler] Client connected sessionId={} clientWsId={}", sessionId, clientSession.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession clientSession, BinaryMessage message) {
        String sessionId = (String) clientSession.getAttributes().get(WebSocketConstants.SESSION_ID_ATTRIBUTE);
        if (sessionId == null) {
            return;
        }
        pipelineService.forwardAudioChunk(sessionId, message.getPayload());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession clientSession, org.springframework.web.socket.CloseStatus status) {
        String sessionId = (String) clientSession.getAttributes().get(WebSocketConstants.SESSION_ID_ATTRIBUTE);
        if (sessionId != null) {
            pipelineService.onClientDisconnected(sessionId, status);
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String sessionId = (String) session.getAttributes().get(WebSocketConstants.SESSION_ID_ATTRIBUTE);
        log.error("[Handler] Transport error sessionId={}", sessionId, exception);
        if (sessionId != null) {
            pipelineService.onClientDisconnected(sessionId, RelayCloseStatus.SERVER_ERROR);
        }
    }
}
