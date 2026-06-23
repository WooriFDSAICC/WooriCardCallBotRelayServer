/**
 *
 *
 * <pre>
 * <b>Description  : CTI 상담원 에스컬레이션 요청</b>
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

import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.session.VoiceSessionEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEscalationService {

    private final CtiEscalationOutboxService ctiEscalationOutboxService;

    public void triggerAgentEscalation(VoiceSessionEntry entry, SessionState state, FdsEvent triggerEvent) {
        log.warn(
                "[Escalation] Agent handoff requested registryKey={} direction={} status={} fdsFlag={} eventType={} reason={}",
                entry.getRegistryKey(),
                entry.getDirection(),
                state.getStatus(),
                state.getFdsFlag(),
                triggerEvent.getEventType(),
                triggerEvent.getReason()
        );
        ctiEscalationOutboxService.triggerEscalation(entry, state, triggerEvent);
    }
}
