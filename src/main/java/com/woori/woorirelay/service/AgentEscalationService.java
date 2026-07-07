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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class AgentEscalationService {

    private final CtiEscalationOutboxService ctiEscalationOutboxService;
    private final ExecutorService ctiEscalationExecutor;

    public AgentEscalationService(
            CtiEscalationOutboxService ctiEscalationOutboxService,
            @Qualifier("ctiEscalationExecutor") ExecutorService ctiEscalationExecutor
    ) {
        this.ctiEscalationOutboxService = ctiEscalationOutboxService;
        this.ctiEscalationExecutor = ctiEscalationExecutor;
    }

    /**
     * CTI 에스컬레이션 요청. HTTP 호출(최대 수 초)이 세션 종료/백엔드 read 스레드를 블로킹하지 않도록
     * 전용 executor 에서 비동기 실행한다. state/event 는 호출 시점 스냅샷이라 세션 종료와 무관하게 유효.
     */
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
        ctiEscalationExecutor.execute(() -> {
            try {
                ctiEscalationOutboxService.triggerEscalation(entry, state, triggerEvent);
            } catch (Exception ex) {
                log.error("[Escalation] Async CTI escalation failed registryKey={}", entry.getRegistryKey(), ex);
            }
        });
    }
}
