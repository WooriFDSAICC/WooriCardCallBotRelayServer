package com.woori.woorirelay.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

/**
 * FastAPI AI Gateway가 WebSocket Text 프레임으로 반환하는 실시간 분석 결과 DTO.
 *
 * 예시:
 * {
 *   "status": "PROCESSING",
 *   "event": "STT_PARTIAL",
 *   "fds_flag": "NORMAL",
 *   "text": "카드 분실 신고",
 *   "stt_text": "카드 분실 신고",
 *   "fds_score": 0.12,
 *   "metadata": { "speaker": "customer" }
 * }
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FastApiStreamResult {

    @JsonProperty("status")
    private String status;

    @JsonProperty("event")
    private String event;

    @JsonProperty("fds_flag")
    private String fdsFlag;

    @JsonProperty("stt_text")
    @JsonAlias("text")
    private String sttText;

    @JsonProperty("fds_score")
    private Double fdsScore;

    @JsonProperty("reason")
    private String reason;

    @JsonProperty("metadata")
    private Map<String, Object> metadata;

    public FdsEvent toFdsEvent(String sessionId) {
        return FdsEvent.builder()
                .sessionId(sessionId)
                .eventType(event)
                .fdsFlag(fdsFlag)
                .sttText(sttText)
                .fdsScore(fdsScore)
                .reason(reason)
                .metadata(metadata)
                .timestamp(java.time.Instant.now())
                .build();
    }
}
