/**
 *
 *
 * <pre>
 * <b>Description  : AI·백엔드 연동 계약 상수 SSOT</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.constant
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

package com.woori.woorirelay.constant;

import lombok.experimental.UtilityClass;

/**
 * AI팀·백엔드 간 연동 계약 상수 (스키마 v1.0) — Relay 연동 SSOT.
 * Python Gateway/FdsGateway {@code integration_contract.py} 와 동기화 유지.
 *
 * <p>Kafka 토픽·이벤트 타입·스키마 버전은 이 클래스를 기준으로 한다.
 * {@link RelayConstants}, {@link StreamEventTypes}는 내부 호환용 위임 래퍼다.
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

    // ── Call Direction (Python integration_contract.py 와 동기화) ──
    public static final String CALL_DIRECTION_INBOUND = "INBOUND";
    public static final String CALL_DIRECTION_OUTBOUND = "OUTBOUND";

    public static final String SESSION_REDIS_KEY_PREFIX = "wooricard:session:";
}
