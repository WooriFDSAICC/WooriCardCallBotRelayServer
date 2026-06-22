/**
 *
 *
 * <pre>
 * <b>Description  : CTI Outbox 재시도 스케줄러</b>
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

import com.woori.woorirelay.config.RelayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CtiEscalationRetryScheduler {

    private final CtiEscalationOutboxService outboxService;
    private final RelayProperties relayProperties;

    @Scheduled(fixedDelayString = "${woori.relay.cti.outbox.retry-interval-ms:30000}")
    public void retryPendingEscalations() {
        if (!relayProperties.getCti().getOutbox().isEnabled()) {
            return;
        }
        int processed = outboxService.processDueRetries();
        if (processed > 0) {
            log.info("[CTI-Outbox] Processed {} due retry item(s)", processed);
        }
    }
}
