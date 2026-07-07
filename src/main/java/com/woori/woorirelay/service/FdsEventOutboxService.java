/**
 *
 *
 * <pre>
 * <b>Description  : FDS 이벤트 Kafka 발행 실패 Outbox 저장·재시도</b>
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
 *  2026.07.07        RosieOh     H3 FDS 이벤트 유실 방지 Outbox 추가
 *        </pre>
 */

package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.RelayConstants;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * FDS 이벤트 발행이 실패하면(브로커 장애·타임아웃·직렬화) 이미 직렬화된 페이로드를 Redis 에
 * 저장하고 스케줄러가 재발행한다. at-least-once 를 보장해 사기탐지 입력 이벤트가 유실되지 않게 한다.
 */
@Slf4j
@Service
public class FdsEventOutboxService {

    private static final String F_TOPIC = "topic";
    private static final String F_KEY = "key";
    private static final String F_PAYLOAD = "payload";
    private static final String F_ATTEMPT = "attempt";
    private static final String F_NEXT_RETRY_AT = "nextRetryAt";
    private static final String F_CREATED_AT = "createdAt";
    private static final String F_LAST_ERROR = "lastError";

    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final RelayProperties relayProperties;
    private final Counter publishFailures;
    private final Counter deadLetters;

    public FdsEventOutboxService(
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            RelayProperties relayProperties,
            MeterRegistry meterRegistry
    ) {
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.relayProperties = relayProperties;
        this.publishFailures = Counter.builder("relay.fds_publish_failures_total")
                .description("FDS event Kafka publish failures routed to outbox")
                .register(meterRegistry);
        this.deadLetters = Counter.builder("relay.fds_outbox_dead_total")
                .description("FDS outbox items that exhausted retries")
                .register(meterRegistry);
        Gauge.builder("relay.fds_outbox_pending", this, FdsEventOutboxService::pendingCount)
                .description("Pending FDS outbox items awaiting republish")
                .register(meterRegistry);
    }

    /** 발행 실패한 이벤트를 Outbox 에 적재한다. */
    public void enqueueFailed(String topic, String partitionKey, String payload, String error) {
        publishFailures.increment();
        RelayProperties.KafkaOutbox config = relayProperties.getKafkaOutbox();
        if (!config.isEnabled()) {
            log.error("[FDS-Outbox] Publish failed and outbox disabled — event LOST topic={} key={} error={}",
                    topic, partitionKey, error);
            return;
        }
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant nextRetryAt = now.plusMillis(config.getInitialRetryDelayMs());
        Map<String, String> item = Map.of(
                F_TOPIC, topic,
                F_KEY, partitionKey == null ? "" : partitionKey,
                F_PAYLOAD, payload,
                F_ATTEMPT, "0",
                F_NEXT_RETRY_AT, Long.toString(nextRetryAt.toEpochMilli()),
                F_CREATED_AT, now.toString(),
                F_LAST_ERROR, error == null ? "" : error
        );
        try {
            redisTemplate.opsForHash().putAll(itemKey(id), item);
            redisTemplate.expire(itemKey(id), RelayConstants.FDS_OUTBOX_TTL_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForZSet().add(queueKey(), id, nextRetryAt.toEpochMilli());
            log.warn("[FDS-Outbox] Enqueued id={} topic={} nextRetryAt={}", id, topic, nextRetryAt);
        } catch (Exception ex) {
            log.error("[FDS-Outbox] Enqueue to Redis failed — event LOST topic={} key={}", topic, partitionKey, ex);
        }
    }

    public int processDueRetries() {
        RelayProperties.KafkaOutbox config = relayProperties.getKafkaOutbox();
        if (!config.isEnabled()) {
            return 0;
        }
        long now = Instant.now().toEpochMilli();
        Set<String> dueIds;
        try {
            dueIds = redisTemplate.opsForZSet().rangeByScore(queueKey(), 0, now, 0, config.getBatchSize());
        } catch (Exception ex) {
            log.error("[FDS-Outbox] Redis scan failed", ex);
            return 0;
        }
        if (dueIds == null || dueIds.isEmpty()) {
            return 0;
        }
        int processed = 0;
        for (String id : dueIds) {
            if (processRetry(id, config)) {
                processed++;
            }
        }
        return processed;
    }

    public long pendingCount() {
        try {
            Long count = redisTemplate.opsForZSet().size(queueKey());
            return count != null ? count : 0L;
        } catch (Exception ex) {
            return 0L;
        }
    }

    private boolean processRetry(String id, RelayProperties.KafkaOutbox config) {
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(itemKey(id));
        if (hash == null || hash.isEmpty()) {
            removeFromQueue(id);
            return false;
        }
        String topic = str(hash, F_TOPIC);
        String key = str(hash, F_KEY);
        String payload = str(hash, F_PAYLOAD);
        int attempt = intVal(hash, F_ATTEMPT);

        try {
            kafkaTemplate.send(topic, key.isBlank() ? null : key, payload)
                    .get(config.getSendTimeoutMs(), TimeUnit.MILLISECONDS);
            redisTemplate.delete(itemKey(id));
            removeFromQueue(id);
            log.info("[FDS-Outbox] Republished id={} topic={} attempt={}", id, topic, attempt + 1);
            return true;
        } catch (Exception ex) {
            int next = attempt + 1;
            if (next >= config.getMaxAttempts()) {
                deadLetters.increment();
                moveToDeadLetter(id, hash, ex.getMessage());
                removeFromQueue(id);
                log.error("[FDS-Outbox] Max attempts exceeded id={} topic={} attempts={} — moved to dead",
                        id, topic, next);
                return false;
            }
            Instant nextRetryAt = Instant.now().plusMillis(config.retryDelayMs(next));
            redisTemplate.opsForHash().put(itemKey(id), F_ATTEMPT, Integer.toString(next));
            redisTemplate.opsForHash().put(itemKey(id), F_NEXT_RETRY_AT, Long.toString(nextRetryAt.toEpochMilli()));
            redisTemplate.opsForHash().put(itemKey(id), F_LAST_ERROR, ex.getMessage() == null ? "" : ex.getMessage());
            redisTemplate.opsForZSet().add(queueKey(), id, nextRetryAt.toEpochMilli());
            log.warn("[FDS-Outbox] Republish failed id={} attempt={} nextRetryAt={}", id, next, nextRetryAt);
            return false;
        }
    }

    private void moveToDeadLetter(String id, Map<Object, Object> hash, String lastError) {
        String deadKey = keyPrefix() + "dead:" + id;
        redisTemplate.opsForHash().putAll(deadKey, hash);
        redisTemplate.opsForHash().put(deadKey, F_LAST_ERROR, lastError == null ? "" : lastError);
        redisTemplate.expire(deadKey, RelayConstants.FDS_OUTBOX_TTL_HOURS, TimeUnit.HOURS);
        redisTemplate.delete(itemKey(id));
    }

    private void removeFromQueue(String id) {
        redisTemplate.opsForZSet().remove(queueKey(), id);
    }

    private static String str(Map<Object, Object> hash, String field) {
        Object v = hash.get(field);
        return v == null ? "" : v.toString();
    }

    private static int intVal(Map<Object, Object> hash, String field) {
        try {
            return Integer.parseInt(str(hash, field));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private String keyPrefix() {
        return relayProperties.getKafkaOutbox().getRedisKeyPrefix();
    }

    private String queueKey() {
        return keyPrefix() + "queue";
    }

    private String itemKey(String id) {
        return keyPrefix() + "item:" + id;
    }
}
