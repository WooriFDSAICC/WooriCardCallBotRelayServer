package com.woori.woorirelay.model;

public enum FdsFlag {
    NORMAL,
    WARNING,
    CRITICAL;

    public static FdsFlag from(String value) {
        if (value == null || value.isBlank()) {
            return NORMAL;
        }
        try {
            return FdsFlag.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NORMAL;
        }
    }
}
