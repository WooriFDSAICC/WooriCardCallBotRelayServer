package com.woori.woorirelay.model;

import com.woori.woorirelay.constant.StreamEventTypes;
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
    private String eventType;
    private String fdsFlag;
    private String sttText;
    private Double fdsScore;
    private String reason;
    private Instant timestamp;
    private Map<String, Object> metadata;
    @Builder.Default
    private String schemaVersion = "1.0";

    public boolean requiresEscalation() {
        if (StreamEventTypes.AGENT_ESCALATION.equalsIgnoreCase(eventType)) {
            return true;
        }
        return FdsFlag.CRITICAL.name().equalsIgnoreCase(fdsFlag);
    }
}
