package com.woori.woorirelay.constant;

import lombok.experimental.UtilityClass;

@UtilityClass
public final class TerminationReason {

    public static final String BINARY_CHUNK_EXCEEDS_LIMIT = "Binary chunk exceeds limit";
    public static final String AUDIO_FORWARD_FAILURE = "Audio forward failure";
    public static final String FASTAPI_CONNECTION_FAILURE = "FastAPI connection failure";
    public static final String FASTAPI_BACKEND_DISCONNECTED = "FastAPI backend disconnected";
    public static final String FDS_ESCALATION = "FDS escalation";
}
