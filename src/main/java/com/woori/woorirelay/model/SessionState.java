package com.woori.woorirelay.model;

import com.woori.woorirelay.constant.RedisHashFields;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class SessionState {

    String sessionId;
    SessionStatus status;
    FdsFlag fdsFlag;
    String lastEvent;
    String lastSttText;
    Instant updatedAt;

    public Map<String, String> toHashFields() {
        return Map.of(
                RedisHashFields.SESSION_ID, sessionId,
                RedisHashFields.STATUS, status.name(),
                RedisHashFields.FDS_FLAG, fdsFlag.name(),
                RedisHashFields.LAST_EVENT, lastEvent != null ? lastEvent : "",
                RedisHashFields.LAST_STT_TEXT, lastSttText != null ? lastSttText : "",
                RedisHashFields.UPDATED_AT, updatedAt.toString()
        );
    }

    public static SessionState initial(String sessionId) {
        return SessionState.builder()
                .sessionId(sessionId)
                .status(SessionStatus.CALL_CONNECTED)
                .fdsFlag(FdsFlag.NORMAL)
                .lastEvent("")
                .lastSttText("")
                .updatedAt(Instant.now())
                .build();
    }

    public static SessionState fromHash(String sessionId, Map<Object, Object> hash) {
        if (hash == null || hash.isEmpty()) {
            return initial(sessionId);
        }
        return SessionState.builder()
                .sessionId(sessionId)
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
        return value == null ? "" : value.toString();
    }
}
