package com.woori.woorirelay.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class RelayConstants {

    public static final long FASTAPI_CONNECT_TIMEOUT_SECONDS = 10L;
    public static final long REDIS_SESSION_TTL_HOURS = 24L;

    public static final String DEFAULT_KAFKA_TOPIC = "wooricard-fds-events";
    public static final String DEFAULT_REDIS_SESSION_KEY_PREFIX = "wooricard:session:";
    public static final String DEFAULT_FASTAPI_WS_BASE_URL = "ws://localhost:8000/v1/stream";

    public static final int DEFAULT_MAX_BINARY_CHUNK_BYTES = 65_536;
    public static final int DEFAULT_WS_TEXT_BUFFER_SIZE = 65_536;
    public static final int DEFAULT_WS_BINARY_BUFFER_SIZE = 65_536;
}
