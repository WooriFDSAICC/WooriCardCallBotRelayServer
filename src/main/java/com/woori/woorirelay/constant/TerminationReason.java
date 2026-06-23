/**
 *
 *
 * <pre>
 * <b>Description  : 세션 종료 사유 상수</b>
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
public final class TerminationReason {

    public static final String BINARY_CHUNK_EXCEEDS_LIMIT = "Binary chunk exceeds limit";
    public static final String AUDIO_FORWARD_FAILURE = "Audio forward failure";
    public static final String FASTAPI_CONNECTION_FAILURE = "FastAPI connection failure";
    public static final String FASTAPI_BACKEND_DISCONNECTED = "FastAPI backend disconnected";
    public static final String FDS_ESCALATION = "FDS escalation";
}
