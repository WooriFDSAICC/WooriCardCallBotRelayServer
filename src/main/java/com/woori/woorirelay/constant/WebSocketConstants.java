/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket 경로 및 속성 상수</b>
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
public final class WebSocketConstants {

    public static final String VOICE_STREAM_PATH = "/voice/{sessionId}";
    public static final String VOICE_INBOUND_PATH = "/voice/inbound/{sessionId}";
    public static final String VOICE_OUTBOUND_PATH = "/voice/outbound/{sessionId}";

    public static final String SESSION_ID_ATTRIBUTE = "sessionId";
    public static final String REGISTRY_KEY_ATTRIBUTE = "registryKey";
    public static final String CALL_DIRECTION_ATTRIBUTE = "callDirection";
    public static final String CAMPAIGN_ID_ATTRIBUTE = "campaignId";
    public static final String PATH_SEPARATOR = "/";

    public static final int CLOSE_CODE_ESCALATION = 4001;
    public static final int CLOSE_CODE_DUPLICATE_SESSION = 4002;

    public static final String CLOSE_REASON_ESCALATION = "Agent escalation";
    public static final String CLOSE_REASON_DUPLICATE_SESSION = "Session already active";
}
