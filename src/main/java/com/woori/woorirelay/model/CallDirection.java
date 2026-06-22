/**
 *
 *
 * <pre>
 * <b>Description  : 통화 방향 열거형 (INBOUND/OUTBOUND)</b>
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

public enum CallDirection {
    INBOUND,
    OUTBOUND;

    public static CallDirection from(String value) {
        if (value == null || value.isBlank()) {
            return INBOUND;
        }
        try {
            return CallDirection.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return INBOUND;
        }
    }

    public String pathSegment() {
        return name().toLowerCase();
    }
}
