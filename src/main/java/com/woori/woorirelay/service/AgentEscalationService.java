package com.woori.woorirelay.service;

import com.woori.woorirelay.constant.CtiConstants;
import com.woori.woorirelay.integration.cti.CtiEscalationRequest;
import com.woori.woorirelay.integration.cti.CtiEscalationResponse;
import com.woori.woorirelay.integration.cti.CtiRoutingClient;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AgentEscalationService {

    private final CtiRoutingClient ctiRoutingClient;

    public void triggerAgentEscalation(String sessionId, SessionState state, FdsEvent triggerEvent) {
        log.warn(
                "[Escalation] Agent handoff requested sessionId={} status={} fdsFlag={} eventType={} reason={}",
                sessionId,
                state.getStatus(),
                state.getFdsFlag(),
                triggerEvent.getEventType(),
                triggerEvent.getReason()
        );

        CtiEscalationRequest request = CtiEscalationRequest.builder()
                .sessionId(sessionId)
                .priority(CtiConstants.PRIORITY_HIGH)
                .fdsFlag(state.getFdsFlag().name())
                .lastSttText(state.getLastSttText())
                .reason(triggerEvent.getReason())
                .eventType(triggerEvent.getEventType())
                .build();

        CtiEscalationResponse response = ctiRoutingClient.enqueueEscalation(request);
        if (!response.isAccepted()) {
            log.error("[Escalation] CTI rejected sessionId={} message={}", sessionId, response.getMessage());
        }
    }
}
