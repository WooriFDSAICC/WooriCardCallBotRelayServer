/**
 *
 *
 * <pre>
 * <b>Description  : Relay 기본 상수</b>
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

@UtilityClass
public final class RelayConstants {

    public static final long FASTAPI_CONNECT_TIMEOUT_SECONDS = 10L;
    public static final long REDIS_SESSION_TTL_HOURS = 24L;

    public static final String DEFAULT_KAFKA_TOPIC = IntegrationContracts.TOPIC_FDS_EVENTS;
    public static final String DEFAULT_REDIS_SESSION_KEY_PREFIX = "wooricard:session:";
    public static final String DEFAULT_FASTAPI_WS_BASE_URL = "ws://localhost:8000/v1/stream";
    public static final String DEFAULT_FASTAPI_WS_INBOUND_BASE_URL = "ws://localhost:8000/v1/stream/inbound";
    public static final String DEFAULT_FASTAPI_WS_OUTBOUND_BASE_URL = "ws://localhost:8000/v1/stream/outbound";

    public static final int DEFAULT_MAX_BINARY_CHUNK_BYTES = 65_536;
    public static final int DEFAULT_WS_TEXT_BUFFER_SIZE = 65_536;
    public static final int DEFAULT_WS_BINARY_BUFFER_SIZE = 65_536;

    public static final long CTI_OUTBOX_TTL_HOURS = 24L;
    public static final long CTI_ESCALATION_DONE_TTL_HOURS = 24L;
}
