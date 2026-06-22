/**
 *
 *
 * <pre>
 * <b>Description  : 분산 세션 소유권 heartbeat 갱신</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.registry
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

package com.woori.woorirelay.registry;

import com.woori.woorirelay.config.RelayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DistributedSessionHeartbeatScheduler {

    private final DistributedSessionOwnershipService ownershipService;
    private final VoiceSessionRegistry sessionRegistry;
    private final RelayProperties relayProperties;

    @Scheduled(fixedDelayString = "${woori.relay.distributed-session.heartbeat-interval-ms:20000}")
    public void refreshOwnershipHeartbeats() {
        if (!relayProperties.getDistributedSession().isEnabled()) {
            return;
        }
        ownershipService.refreshInstanceHeartbeat();
        sessionRegistry.activeEntries().forEach(entry ->
                ownershipService.refreshOwnership(entry.getDirection(), entry.getSessionId()));
    }
}
