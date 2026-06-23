/**
 *
 *
 * <pre>
 * <b>Description  : Redis 세션 상태 모델</b>
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

import com.woori.woorirelay.constant.RedisHashFields;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class SessionState {

    String sessionId;
    CallDirection direction;
    String campaignId;
    SessionStatus status;
    FdsFlag fdsFlag;
    String lastEvent;
    String lastSttText;
    Instant updatedAt;

    public Map<String, String> toHashFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put(RedisHashFields.SESSION_ID, sessionId);
        fields.put(RedisHashFields.CALL_DIRECTION, direction != null ? direction.name() : CallDirection.INBOUND.name());
        fields.put(RedisHashFields.CAMPAIGN_ID, campaignId != null ? campaignId : "");
        fields.put(RedisHashFields.STATUS, status.name());
        fields.put(RedisHashFields.FDS_FLAG, fdsFlag.name());
        fields.put(RedisHashFields.LAST_EVENT, lastEvent != null ? lastEvent : "");
        fields.put(RedisHashFields.LAST_STT_TEXT, lastSttText != null ? lastSttText : "");
        fields.put(RedisHashFields.UPDATED_AT, updatedAt.toString());
        return fields;
    }

    public static SessionState initial(String sessionId, CallDirection direction, String campaignId) {
        return SessionState.builder()
                .sessionId(sessionId)
                .direction(direction != null ? direction : CallDirection.INBOUND)
                .campaignId(campaignId)
                .status(SessionStatus.CALL_CONNECTED)
                .fdsFlag(FdsFlag.NORMAL)
                .lastEvent("")
                .lastSttText("")
                .updatedAt(Instant.now())
                .build();
    }

    public static SessionState fromHash(String sessionId, Map<Object, Object> hash) {
        if (hash == null || hash.isEmpty()) {
            return initial(sessionId, CallDirection.INBOUND, null);
        }
        return SessionState.builder()
                .sessionId(sessionId)
                .direction(CallDirection.from(stringValue(hash.get(RedisHashFields.CALL_DIRECTION))))
                .campaignId(stringValue(hash.get(RedisHashFields.CAMPAIGN_ID)))
                .status(parseStatus(hash.get(RedisHashFields.STATUS)))
                .fdsFlag(FdsFlag.from(stringValue(hash.get(RedisHashFields.FDS_FLAG))))
                .lastEvent(stringValue(hash.get(RedisHashFields.LAST_EVENT)))
                .lastSttText(stringValue(hash.get(RedisHashFields.LAST_STT_TEXT)))
                .updatedAt(parseInstant(hash.get(RedisHashFields.UPDATED_AT)))
                .build();
    }

    private static SessionStatus parseStatus(Object value) {
        if (value == null) {
            return SessionStatus.CALL_CONNECTED;
        }
        try {
            return SessionStatus.valueOf(value.toString());
        } catch (IllegalArgumentException ex) {
            return SessionStatus.CALL_CONNECTED;
        }
    }

    private static Instant parseInstant(Object value) {
        if (value == null || value.toString().isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception ex) {
            return Instant.now();
        }
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        String text = value.toString();
        return text.isBlank() ? "" : text;
    }
}
