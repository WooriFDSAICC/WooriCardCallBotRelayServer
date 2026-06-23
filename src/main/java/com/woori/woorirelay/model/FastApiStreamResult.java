/**
 *
 *
 * <pre>
 * <b>Description  : Gateway WebSocket JSON 분석 결과 DTO</b>
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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

import com.woori.woorirelay.session.VoiceSessionEntry;

/**
 * FastAPI AI Gateway가 WebSocket Text 프레임으로 반환하는 실시간 분석 결과 DTO.
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

    public FdsEvent toFdsEvent(VoiceSessionEntry entry) {
        return FdsEvent.builder()
                .sessionId(entry.getSessionId())
                .callDirection(entry.getDirection().name())
                .campaignId(entry.getCampaignId())
                .eventType(event)
                .fdsFlag(fdsFlag)
                .sttText(sttText)
                .fdsScore(fdsScore)
                .reason(reason)
                .metadata(metadata)
                .timestamp(java.time.Instant.now())
                .build();
    }

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
