/**
 *
 *
 * <pre>
 * <b>Description  : CTI 에스컬레이션 Outbox 저장 및 재시도</b>
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
import com.woori.woorirelay.constant.CtiConstants;
import com.woori.woorirelay.constant.RelayConstants;
import com.woori.woorirelay.constant.RelayRedisKeys;
import com.woori.woorirelay.integration.cti.CtiEscalationRequest;
import com.woori.woorirelay.integration.cti.CtiEscalationResponse;
import com.woori.woorirelay.integration.cti.CtiRoutingClient;
import com.woori.woorirelay.model.CtiEscalationOutboxEntry;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.session.VoiceSessionEntry;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class CtiEscalationOutboxService {

    private final CtiRoutingClient ctiRoutingClient;
    private final StringRedisTemplate redisTemplate;
    private final RelayProperties relayProperties;
    private final Counter retryCounter;

    public CtiEscalationOutboxService(
            CtiRoutingClient ctiRoutingClient,
            StringRedisTemplate redisTemplate,
            RelayProperties relayProperties,
            MeterRegistry meterRegistry
    ) {
        this.ctiRoutingClient = ctiRoutingClient;
        this.redisTemplate = redisTemplate;
        this.relayProperties = relayProperties;
        this.retryCounter = Counter.builder("relay.cti_escalation_retries")
                .description("CTI escalation retry attempts")
                .register(meterRegistry);
    }

    public void triggerEscalation(VoiceSessionEntry entry, SessionState state, FdsEvent triggerEvent) {
        String registryKey = entry.getRegistryKey();
        if (isCompleted(registryKey)) {
            log.info("[CTI-Outbox] Already completed registryKey={}", registryKey);
            return;
        }

        CtiEscalationRequest request = buildRequest(entry, state, triggerEvent);
        CtiEscalationResponse response = ctiRoutingClient.enqueueEscalation(request);
        if (response.isAccepted()) {
            markCompleted(registryKey);
            log.info("[CTI-Outbox] Escalation accepted registryKey={} queueId={}",
                    registryKey, response.getQueueId());
            return;
        }

        enqueue(request, response.getMessage());
    }

    public int processDueRetries() {
        RelayProperties.CtiOutbox config = relayProperties.getCti().getOutbox();
        if (!config.isEnabled()) {
            return 0;
        }

        long now = Instant.now().toEpochMilli();
        Set<String> dueKeys = redisTemplate.opsForZSet()
                .rangeByScore(queueKey(), 0, now, 0, config.getBatchSize());
        if (dueKeys == null || dueKeys.isEmpty()) {
            return 0;
        }

        int processed = 0;
        for (String registryKey : dueKeys) {
            if (processRetry(registryKey)) {
                processed++;
            }
        }
        return processed;
    }

    public long pendingCount() {
        Long count = redisTemplate.opsForZSet().size(queueKey());
        return count != null ? count : 0L;
    }

    public boolean isCompleted(String registryKey) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(doneKey(registryKey)));
    }

    private boolean processRetry(String registryKey) {
        if (isCompleted(registryKey)) {
            removeFromQueue(registryKey);
            return false;
        }

        Map<Object, Object> hash = redisTemplate.opsForHash().entries(itemKey(registryKey));
        CtiEscalationOutboxEntry entry = CtiEscalationOutboxEntry.fromHash(hash);
        if (entry == null) {
            removeFromQueue(registryKey);
            return false;
        }

        retryCounter.increment();
        CtiEscalationResponse response = ctiRoutingClient.enqueueEscalation(toRequest(entry));
        if (response.isAccepted()) {
            markCompleted(registryKey);
            removeFromQueue(registryKey);
            redisTemplate.delete(itemKey(registryKey));
            log.info("[CTI-Outbox] Retry succeeded registryKey={} attempt={}",
                    registryKey, entry.getAttemptCount() + 1);
            return true;
        }

        int nextAttempt = entry.getAttemptCount() + 1;
        RelayProperties.CtiOutbox config = relayProperties.getCti().getOutbox();
        if (nextAttempt >= config.getMaxAttempts()) {
            moveToDeadLetter(entry, response.getMessage());
            removeFromQueue(registryKey);
            log.error("[CTI-Outbox] Max attempts exceeded registryKey={} attempts={}",
                    registryKey, nextAttempt);
            return false;
        }

        Instant nextRetryAt = Instant.now().plusMillis(config.retryDelayMs(nextAttempt));
        CtiEscalationOutboxEntry updated = entry.toBuilder()
                .attemptCount(nextAttempt)
                .nextRetryAt(nextRetryAt)
                .lastError(response.getMessage())
                .build();
        redisTemplate.opsForHash().putAll(itemKey(registryKey), updated.toHashFields());
        redisTemplate.opsForZSet().add(queueKey(), registryKey, nextRetryAt.toEpochMilli());
        redisTemplate.expire(itemKey(registryKey), RelayConstants.CTI_OUTBOX_TTL_HOURS, TimeUnit.HOURS);
        log.warn("[CTI-Outbox] Retry failed registryKey={} attempt={} nextRetryAt={}",
                registryKey, nextAttempt, nextRetryAt);
        return false;
    }

    private void enqueue(CtiEscalationRequest request, String lastError) {
        RelayProperties.CtiOutbox config = relayProperties.getCti().getOutbox();
        if (!config.isEnabled()) {
            log.error("[CTI-Outbox] Escalation failed and outbox disabled registryKey={} error={}",
                    request.getRegistryKey(), lastError);
            return;
        }

        String registryKey = request.getRegistryKey();
        Instant now = Instant.now();
        Instant nextRetryAt = now.plusMillis(config.getInitialRetryDelayMs());

        CtiEscalationOutboxEntry entry = CtiEscalationOutboxEntry.builder()
                .registryKey(registryKey)
                .sessionId(request.getSessionId())
                .callDirection(request.getCallDirection().name())
                .campaignId(request.getCampaignId())
                .priority(request.getPriority())
                .fdsFlag(request.getFdsFlag())
                .lastSttText(request.getLastSttText())
                .reason(request.getReason())
                .eventType(request.getEventType())
                .attemptCount(0)
                .nextRetryAt(nextRetryAt)
                .createdAt(now)
                .lastError(lastError)
                .build();

        redisTemplate.opsForHash().putAll(itemKey(registryKey), entry.toHashFields());
        redisTemplate.opsForZSet().add(queueKey(), registryKey, nextRetryAt.toEpochMilli());
        redisTemplate.expire(itemKey(registryKey), RelayConstants.CTI_OUTBOX_TTL_HOURS, TimeUnit.HOURS);
        log.warn("[CTI-Outbox] Enqueued registryKey={} nextRetryAt={}", registryKey, nextRetryAt);
    }

    private void markCompleted(String registryKey) {
        redisTemplate.opsForValue().set(
                doneKey(registryKey),
                Instant.now().toString(),
                RelayConstants.CTI_ESCALATION_DONE_TTL_HOURS,
                TimeUnit.HOURS
        );
        removeFromQueue(registryKey);
        redisTemplate.delete(itemKey(registryKey));
    }

    private void moveToDeadLetter(CtiEscalationOutboxEntry entry, String lastError) {
        CtiEscalationOutboxEntry dead = entry.toBuilder()
                .lastError(lastError)
                .build();
        String deadKey = deadLetterKey(entry.getRegistryKey());
        redisTemplate.opsForHash().putAll(deadKey, dead.toHashFields());
        redisTemplate.expire(deadKey, RelayConstants.CTI_OUTBOX_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.delete(itemKey(entry.getRegistryKey()));
    }

    private void removeFromQueue(String registryKey) {
        redisTemplate.opsForZSet().remove(queueKey(), registryKey);
    }

    private CtiEscalationRequest buildRequest(VoiceSessionEntry entry, SessionState state, FdsEvent triggerEvent) {
        return CtiEscalationRequest.builder()
                .registryKey(entry.getRegistryKey())
                .sessionId(entry.getSessionId())
                .callDirection(entry.getDirection())
                .campaignId(entry.getCampaignId())
                .priority(CtiConstants.PRIORITY_HIGH)
                .fdsFlag(state.getFdsFlag().name())
                .lastSttText(state.getLastSttText())
                .reason(triggerEvent.getReason())
                .eventType(triggerEvent.getEventType())
                .build();
    }

    private CtiEscalationRequest toRequest(CtiEscalationOutboxEntry entry) {
        return CtiEscalationRequest.builder()
                .registryKey(entry.getRegistryKey())
                .sessionId(entry.getSessionId())
                .callDirection(com.woori.woorirelay.model.CallDirection.from(entry.getCallDirection()))
                .campaignId(entry.getCampaignId())
                .priority(entry.getPriority())
                .fdsFlag(entry.getFdsFlag())
                .lastSttText(entry.getLastSttText())
                .reason(entry.getReason())
                .eventType(entry.getEventType())
                .build();
    }

    private String queueKey() {
        return relayProperties.getCti().getOutbox().getRedisKeyPrefix() + RelayRedisKeys.CTI_OUTBOX_QUEUE_SUFFIX;
    }

    private String itemKey(String registryKey) {
        return relayProperties.getCti().getOutbox().getRedisKeyPrefix()
                + RelayRedisKeys.CTI_OUTBOX_ITEM_SUFFIX + registryKey;
    }

    private String doneKey(String registryKey) {
        return relayProperties.getCti().getOutbox().getRedisKeyPrefix()
                + RelayRedisKeys.CTI_ESCALATION_DONE_SUFFIX + registryKey;
    }

    private String deadLetterKey(String registryKey) {
        return relayProperties.getCti().getOutbox().getRedisKeyPrefix()
                + RelayRedisKeys.CTI_OUTBOX_DEAD_SUFFIX + registryKey;
    }
}
