package com.woori.woorirelay.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.StreamEventTypes;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * STT / FDS 분석 결과를 Kafka 'wooricard-fds-events' 토픽으로 비동기 발행.
 * 후행 룰 엔진 및 MLOps 피처 스토어가 컨슘한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final RelayProperties relayProperties;

    public void publishFdsEvent(FdsEvent event) {
        String topic = relayProperties.getKafkaTopic();
        String payload;
        try {
            payload = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException ex) {
            log.error("[Kafka] JSON serialization failed sessionId={} eventType={}",
                    event.getSessionId(), event.getEventType(), ex);
            return;
        }

        // sessionId를 partition key로 사용 → 동일 통화 이벤트 순서 보장
        CompletableFuture<SendResult<String, String>> future =
                kafkaTemplate.send(topic, event.getSessionId(), payload);

        future.whenComplete((result, throwable) -> {
            if (throwable != null) {
                log.error("[Kafka] Publish failed sessionId={} topic={}",
                        event.getSessionId(), topic, throwable);
                return;
            }
            log.debug("[Kafka] Published sessionId={} topic={} offset={} eventType={} stt={}",
                    event.getSessionId(),
                    topic,
                    result.getRecordMetadata().offset(),
                    event.getEventType(),
                    com.woori.woorirelay.support.PiiMaskingUtil.maskSttText(event.getSttText()));
        });
    }

    /**
     * 통화 종료 피드백 — FdsGateway Feature Store / MLOps 후행 처리용.
     */
    public void publishSessionEnded(String sessionId, String reason, SessionState state) {
        Map<String, Object> metadata = new HashMap<>();
        if (state != null) {
            metadata.put("finalStatus", state.getStatus() != null ? state.getStatus().name() : "");
            metadata.put("finalFdsFlag", state.getFdsFlag() != null ? state.getFdsFlag().name() : "");
            metadata.put("lastEvent", state.getLastEvent() != null ? state.getLastEvent() : "");
        }

        FdsEvent event = FdsEvent.builder()
                .sessionId(sessionId)
                .eventType(StreamEventTypes.SESSION_ENDED)
                .reason(reason)
                .timestamp(Instant.now())
                .metadata(metadata)
                .build();
        publishFdsEvent(event);
    }
}
