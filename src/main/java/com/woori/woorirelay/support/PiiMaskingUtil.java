package com.woori.woorirelay.support;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public final class PiiMaskingUtil {

    private static final Pattern PHONE_PATTERN = Pattern.compile("\\d{3}-?\\d{3,4}-?\\d{4}");

    public static String maskUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            return "***";
        }
        if (userId.length() <= 4) {
            return "*".repeat(userId.length());
        }
        return userId.substring(0, 2) + "****" + userId.substring(userId.length() - 2);
    }

    public static String maskSttText(String text) {
        if (text == null || text.isBlank()) {
            return "";
        }
        String masked = PHONE_PATTERN.matcher(text).replaceAll("***-****-****");
        if (masked.length() <= 4) {
            return "*".repeat(masked.length());
        }
        return masked.substring(0, 4) + "*".repeat(masked.length() - 4);
    }
}
