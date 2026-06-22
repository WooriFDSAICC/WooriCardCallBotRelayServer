/**
 *
 *
 * <pre>
 * <b>Description  : CTI Outbox 엔트리 모델</b>
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

import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Value
@Builder(toBuilder = true)
public class CtiEscalationOutboxEntry {

    String registryKey;
    String sessionId;
    String callDirection;
    String campaignId;
    String priority;
    String fdsFlag;
    String lastSttText;
    String reason;
    String eventType;
    int attemptCount;
    Instant nextRetryAt;
    Instant createdAt;
    String lastError;

    public Map<String, String> toHashFields() {
        Map<String, String> fields = new HashMap<>();
        fields.put("registryKey", registryKey != null ? registryKey : "");
        fields.put("sessionId", sessionId != null ? sessionId : "");
        fields.put("callDirection", callDirection != null ? callDirection : "");
        fields.put("campaignId", campaignId != null ? campaignId : "");
        fields.put("priority", priority != null ? priority : "");
        fields.put("fdsFlag", fdsFlag != null ? fdsFlag : "");
        fields.put("lastSttText", lastSttText != null ? lastSttText : "");
        fields.put("reason", reason != null ? reason : "");
        fields.put("eventType", eventType != null ? eventType : "");
        fields.put("attemptCount", String.valueOf(attemptCount));
        fields.put("nextRetryAt", nextRetryAt != null ? nextRetryAt.toString() : "");
        fields.put("createdAt", createdAt != null ? createdAt.toString() : "");
        fields.put("lastError", lastError != null ? lastError : "");
        return fields;
    }

    public static CtiEscalationOutboxEntry fromHash(Map<Object, Object> hash) {
        if (hash == null || hash.isEmpty()) {
            return null;
        }
        return CtiEscalationOutboxEntry.builder()
                .registryKey(stringValue(hash.get("registryKey")))
                .sessionId(stringValue(hash.get("sessionId")))
                .callDirection(stringValue(hash.get("callDirection")))
                .campaignId(stringValue(hash.get("campaignId")))
                .priority(stringValue(hash.get("priority")))
                .fdsFlag(stringValue(hash.get("fdsFlag")))
                .lastSttText(stringValue(hash.get("lastSttText")))
                .reason(stringValue(hash.get("reason")))
                .eventType(stringValue(hash.get("eventType")))
                .attemptCount(parseInt(hash.get("attemptCount")))
                .nextRetryAt(parseInstant(hash.get("nextRetryAt")))
                .createdAt(parseInstant(hash.get("createdAt")))
                .lastError(stringValue(hash.get("lastError")))
                .build();
    }

    private static String stringValue(Object value) {
        return value == null ? "" : value.toString();
    }

    private static int parseInt(Object value) {
        if (value == null) {
            return 0;
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static Instant parseInstant(Object value) {
        if (value == null || value.toString().isBlank()) {
            return Instant.EPOCH;
        }
        try {
            return Instant.parse(value.toString());
        } catch (Exception ex) {
            return Instant.EPOCH;
        }
    }
}
