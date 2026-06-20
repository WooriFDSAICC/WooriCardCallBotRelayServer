package com.woori.woorirelay.service;

import com.woori.woorirelay.constant.RelayCloseStatus;
import com.woori.woorirelay.constant.TerminationReason;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.support.WebSocketSessionCloser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

/**
 * 세션 에스컬레이션, 종료, WebSocket/Redis 정리 전담 서비스.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceSessionLifecycleService {

    private final RedisStateService redisStateService;
    private final AgentEscalationService agentEscalationService;
    private final KafkaProducerService kafkaProducerService;
    private final VoiceSessionRegistry sessionRegistry;
    private final WebSocketSessionCloser sessionCloser;

    public void escalateAndClose(VoiceSessionEntry entry, SessionState state, FdsEvent event) {
        if (!entry.markEscalatedOnce()) {
            return;
        }

        String sessionId = entry.getSessionId();
        synchronized (entry.getLifecycleLock()) {
            redisStateService.markEscalated(sessionId);
            agentEscalationService.triggerAgentEscalation(sessionId, state, event);
            terminateSession(sessionId, RelayCloseStatus.ESCALATION, TerminationReason.FDS_ESCALATION, true);
        }
    }

    public void terminateSession(String sessionId, CloseStatus status, String reason, boolean fromEscalation) {
        VoiceSessionEntry entry = sessionRegistry.find(sessionId).orElse(null);
        if (entry == null) {
            return;
        }

        synchronized (entry.getLifecycleLock()) {
            if (!entry.markClosedOnce()) {
                return;
            }
            sessionRegistry.remove(sessionId);
            cleanupEntry(entry, status, fromEscalation);
            log.info("[Lifecycle] Session terminated sessionId={} reason={} status={}",
                    sessionId, reason, status);
        }
    }

    public void cleanupOnClientDisconnect(String sessionId, CloseStatus status) {
        log.info("[Lifecycle] Client disconnected sessionId={} status={}", sessionId, status);
        sessionRegistry.remove(sessionId).ifPresent(entry -> cleanupEntry(entry, status, false));
    }

    private void cleanupEntry(VoiceSessionEntry entry, CloseStatus status, boolean fromEscalation) {
        String sessionId = entry.getSessionId();
        SessionState state = redisStateService.getSession(sessionId).orElse(null);
        kafkaProducerService.publishSessionEnded(sessionId, status.toString(), state);

        sessionCloser.closeQuietly(entry.getBackendSession(), status);
        sessionCloser.closeQuietly(entry.getClientSession(), status);

        if (!fromEscalation && !entry.getEscalated().get()) {
            redisStateService.markClosed(sessionId);
        }
    }
}
