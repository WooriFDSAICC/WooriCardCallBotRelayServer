/**
 *
 *
 * <pre>
 * <b>Description  : FDS Outbox 재발행 스케줄러</b>
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
 *  2026.07.07        RosieOh     H3 FDS Outbox 재발행 스케줄러
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
public class FdsEventOutboxScheduler {

    private final FdsEventOutboxService outboxService;
    private final RelayProperties relayProperties;

    @Scheduled(fixedDelayString = "${woori.relay.kafka-outbox.retry-interval-ms:10000}")
    public void retryPending() {
        if (!relayProperties.getKafkaOutbox().isEnabled()) {
            return;
        }
        int processed = outboxService.processDueRetries();
        if (processed > 0) {
            log.info("[FDS-Outbox] Republished {} due item(s)", processed);
        }
    }
}
