/**
 *
 *
 * <pre>
 * <b>Description  : Kafka FDS 이벤트 DTO</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.model
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

package com.woori.woorirelay.model;

import com.woori.woorirelay.constant.IntegrationContracts;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class FdsEvent {

    private String sessionId;
    private String callDirection;
    private String campaignId;
    private String eventType;
    private String fdsFlag;
    private String sttText;
    private Double fdsScore;
    private String reason;
    private Instant timestamp;
    private Map<String, Object> metadata;
    @Builder.Default
    private String schemaVersion = IntegrationContracts.SCHEMA_VERSION;

    public boolean requiresEscalation() {
        if (IntegrationContracts.EVENT_AGENT_ESCALATION.equalsIgnoreCase(eventType)) {
            return true;
        }
        return FdsFlag.CRITICAL.name().equalsIgnoreCase(fdsFlag);
    }
}
