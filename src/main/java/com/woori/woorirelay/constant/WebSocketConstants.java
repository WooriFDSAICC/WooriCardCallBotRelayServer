package com.woori.woorirelay.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class WebSocketConstants {

    public static final String VOICE_STREAM_PATH = "/voice/{sessionId}";
    public static final String SESSION_ID_ATTRIBUTE = "sessionId";
    public static final String PATH_SEPARATOR = "/";

    public static final int CLOSE_CODE_ESCALATION = 4001;
    public static final int CLOSE_CODE_DUPLICATE_SESSION = 4002;

    public static final String CLOSE_REASON_ESCALATION = "Agent escalation";
    public static final String CLOSE_REASON_DUPLICATE_SESSION = "Session already active";
}
