/**
 *
 *
 * <pre>
 * <b>Description  : 세션 에스컬레이션 및 종료 라이프사이클</b>
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

import com.woori.woorirelay.constant.RelayCloseStatus;
import com.woori.woorirelay.constant.TerminationReason;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.registry.DistributedSessionOwnershipService;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.support.WebSocketSessionCloser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.CloseStatus;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceSessionLifecycleService {

    private final RedisStateService redisStateService;
    private final AgentEscalationService agentEscalationService;
    private final KafkaProducerService kafkaProducerService;
    private final VoiceSessionRegistry sessionRegistry;
    private final WebSocketSessionCloser sessionCloser;
    private final DistributedSessionOwnershipService ownershipService;

    public void escalateAndClose(VoiceSessionEntry entry, SessionState state, FdsEvent event) {
        if (!entry.markEscalatedOnce()) {
            return;
        }

        synchronized (entry.getLifecycleLock()) {
            redisStateService.markEscalated(entry.getDirection(), entry.getSessionId());
            agentEscalationService.triggerAgentEscalation(entry, state, event);
            terminateSession(entry.getRegistryKey(), RelayCloseStatus.ESCALATION, TerminationReason.FDS_ESCALATION, true);
        }
    }

    public void terminateSession(String registryKey, CloseStatus status, String reason, boolean fromEscalation) {
        VoiceSessionEntry entry = sessionRegistry.find(registryKey).orElse(null);
        if (entry == null) {
            return;
        }

        synchronized (entry.getLifecycleLock()) {
            if (!entry.markClosedOnce()) {
                return;
            }
            sessionRegistry.remove(registryKey);
            cleanupEntry(entry, status, fromEscalation);
            log.info("[Lifecycle] Session terminated registryKey={} reason={} status={}",
                    registryKey, reason, status);
        }
    }

    public void cleanupOnClientDisconnect(String registryKey, CloseStatus status) {
        log.info("[Lifecycle] Client disconnected registryKey={} status={}", registryKey, status);
        sessionRegistry.remove(registryKey).ifPresent(entry -> cleanupEntry(entry, status, false));
    }

    private void cleanupEntry(VoiceSessionEntry entry, CloseStatus status, boolean fromEscalation) {
        SessionState state = redisStateService.getSession(entry.getDirection(), entry.getSessionId()).orElse(null);
        kafkaProducerService.publishSessionEnded(entry, status.toString(), state);

        sessionCloser.closeQuietly(entry.getBackendSession(), status);
        sessionCloser.closeQuietly(entry.getClientSession(), status);

        if (!fromEscalation && !entry.getEscalated().get()) {
            redisStateService.markClosed(entry.getDirection(), entry.getSessionId());
        }
        ownershipService.release(entry.getDirection(), entry.getSessionId());
    }
}
