/**
 *
 *
 * <pre>
 * <b>Description  : 로그용 PII 마스킹 유틸리티</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.support
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

package com.woori.woorirelay.support;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public final class PiiMaskingUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{3}-?\\d{3,4}-?\\d{4}");
    private static final Pattern CARD_PATTERN = Pattern.compile(
            "\\b(?:\\d[ -]?){13,19}\\b"
    );
    private static final Pattern LONG_DIGIT_PATTERN = Pattern.compile("\\d{10,}");

    public static String maskUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "***";
        }
        if (userId.length() <= 4) {
            return "*".repeat(userId.length());
        }
        return userId.substring(0, 2) + "****" + userId.substring(userId.length() - 2);
    }

    /**
     * STT 로그용 마스킹 — 패턴 치환 후 발화 앞 4자만 남기고 나머지는 * 처리.
     * 콜봇 운영 로그에서 고객 발화 전체가 노출되지 않도록 한다.
     */
    public static String maskSttText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String masked = PHONE_PATTERN.matcher(text).replaceAll("***-****-****");
        masked = CARD_PATTERN.matcher(masked).replaceAll("****-****-****-****");
        masked = LONG_DIGIT_PATTERN.matcher(masked).replaceAll("**********");
        if (masked.length() <= 4) {
            return "*".repeat(masked.length());
        }
        return masked.substring(0, 4) + "*".repeat(masked.length() - 4);
    }

    /** JSON 페이로드 등 로그 출력용 — 패턴 마스킹 후 길이 제한. */
    public static String maskForLog(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String masked = maskSttText(text);
        int maxLength = 120;
        if (masked.length() <= maxLength) {
            return masked;
        }
        return masked.substring(0, maxLength) + "...(truncated)";
    }
}
