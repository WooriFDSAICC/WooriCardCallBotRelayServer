package com.woori.woorirelay.constant;

import lombok.experimental.UtilityClass;

/**
 * AI팀·백엔드 간 연동 계약 상수 (스키마 v1.0).
 * Python Gateway/FdsGateway {@code integration_contract.py} 와 동기화 유지.
 */
@UtilityClass
public final class IntegrationContracts {

    public static final String SCHEMA_VERSION = "1.0";

    // ── Kafka Topics ──
    public static final String TOPIC_FDS_EVENTS = "wooricard-fds-events";
    public static final String TOPIC_FDS_SCORES = "wooricard-fds-scores";
    public static final String TOPIC_FDS_ACTIONS = "wooricard-fds-actions";
    public static final String TOPIC_FDS_DLQ = "wooricard-fds-events-dlq";

    // ── Event Types ──
    public static final String EVENT_STT_PARTIAL = "STT_PARTIAL";
    public static final String EVENT_AGENT_ESCALATION = "AGENT_ESCALATION";
    public static final String EVENT_SESSION_ENDED = "SESSION_ENDED";

    // ── Triton Models (AI팀 배포) ──
    public static final String TRITON_MODEL_STT = "stt_streaming";
    public static final String TRITON_MODEL_ASD = "asd_voiceprint";
    public static final String TRITON_MODEL_FDS = "fds_lgbm";

    // ── Feature Store Redis ──
    public static final String FEATURE_STORE_KEY_PREFIX = "fds:feature:";
}
