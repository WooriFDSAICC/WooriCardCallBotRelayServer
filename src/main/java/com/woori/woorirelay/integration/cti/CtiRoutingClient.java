/**
 *
 *
 * <pre>
 * <b>Description  : CTI/ACD REST 클라이언트</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.integration.cti
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

package com.woori.woorirelay.integration.cti;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.CtiConstants;
import com.woori.woorirelay.model.CallDirection;
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
import java.util.HashMap;
import java.util.Map;

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
                    "[CTI:Mock] Escalation queued registryKey={} direction={} fdsFlag={} stt={}",
                    request.getRegistryKey(),
                    request.getCallDirection(),
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

        Map<String, Object> body = new HashMap<>();
        body.put("sessionId", request.getSessionId());
        body.put("registryKey", request.getRegistryKey());
        body.put("callDirection", request.getCallDirection() != null ? request.getCallDirection().name() : CallDirection.INBOUND.name());
        body.put("campaignId", request.getCampaignId() != null ? request.getCampaignId() : "");
        body.put("priority", request.getPriority());
        body.put("fdsFlag", request.getFdsFlag());
        body.put("lastSttText", request.getLastSttText() != null ? request.getLastSttText() : "");
        body.put("reason", request.getReason() != null ? request.getReason() : "");
        body.put("eventType", request.getEventType() != null ? request.getEventType() : "");

        String url = cti.getBaseUrl() + cti.getEscalationPath();
        try {
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    url,
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            log.info("[CTI] Escalation accepted registryKey={} status={}",
                    request.getRegistryKey(), response.getStatusCode());
            return CtiEscalationResponse.builder()
                    .accepted(response.getStatusCode().is2xxSuccessful())
                    .queueId(extractQueueId(response.getBody()))
                    .message("CTI escalation accepted")
                    .build();
        } catch (RestClientException ex) {
            log.error("[CTI] Escalation failed registryKey={} url={}", request.getRegistryKey(), url, ex);
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
