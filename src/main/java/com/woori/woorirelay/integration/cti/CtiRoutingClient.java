package com.woori.woorirelay.integration.cti;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.CtiConstants;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

/**
 * CTI/ACD 상담원 라우팅 REST 클라이언트.
 * 운영: Contact Center API URL을 woori.relay.cti.base-url에 설정.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CtiRoutingClient {

    private final RelayProperties relayProperties;
    private final RestTemplateBuilder restTemplateBuilder;

    public CtiEscalationResponse enqueueEscalation(CtiEscalationRequest request) {
        RelayProperties.Cti cti = relayProperties.getCti();

        if (!cti.isEnabled()) {
            log.warn(
                    "[CTI:Mock] Escalation queued sessionId={} fdsFlag={} stt={}",
                    request.getSessionId(),
                    request.getFdsFlag(),
                    request.getLastSttText()
            );
            return CtiEscalationResponse.builder()
                    .accepted(true)
                    .queueId("MOCK-QUEUE-001")
                    .message("CTI disabled — mock escalation accepted")
                    .build();
        }

        RestTemplate restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofMillis(cti.getConnectTimeoutMs()))
                .readTimeout(Duration.ofMillis(cti.getReadTimeoutMs()))
                .build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(CtiConstants.HEADER_CORRELATION_ID, request.getSessionId());

        Map<String, Object> body = Map.of(
                "sessionId", request.getSessionId(),
                "priority", request.getPriority(),
                "fdsFlag", request.getFdsFlag(),
                "lastSttText", request.getLastSttText() != null ? request.getLastSttText() : "",
                "reason", request.getReason() != null ? request.getReason() : "",
                "eventType", request.getEventType() != null ? request.getEventType() : ""
        );

        String url = cti.getBaseUrl() + cti.getEscalationPath();
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("[CTI] Escalation accepted sessionId={} status={}", request.getSessionId(), response.getStatusCode());
            return CtiEscalationResponse.builder()
                    .accepted(response.getStatusCode().is2xxSuccessful())
                    .queueId(extractQueueId(response.getBody()))
                    .message("CTI escalation accepted")
                    .build();
        } catch (RestClientException ex) {
            log.error("[CTI] Escalation failed sessionId={} url={}", request.getSessionId(), url, ex);
            return CtiEscalationResponse.builder()
                    .accepted(false)
                    .message(ex.getMessage())
                    .build();
        }
    }

    private String extractQueueId(Map<?, ?> body) {
        if (body == null) {
            return null;
        }
        Object queueId = body.get("queueId");
        return queueId != null ? queueId.toString() : null;
    }
}
