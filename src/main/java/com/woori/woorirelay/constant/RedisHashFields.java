/**
 *
 *
 * <pre>
 * <b>Description  : Redis 세션 Hash 필드명</b>
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
public final class RedisHashFields {

    public static final String SESSION_ID = "session_id";
    public static final String CALL_DIRECTION = "call_direction";
    public static final String CAMPAIGN_ID = "campaign_id";
    public static final String STATUS = "status";
    public static final String FDS_FLAG = "fds_flag";
    public static final String LAST_EVENT = "last_event";
    public static final String LAST_STT_TEXT = "last_stt_text";
    public static final String UPDATED_AT = "updated_at";
}
