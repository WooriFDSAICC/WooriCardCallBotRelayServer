/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket 종료 상태 코드</b>
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
import org.springframework.web.socket.CloseStatus;

@UtilityClass
public final class RelayCloseStatus {

    public static final CloseStatus ESCALATION = new CloseStatus(
            WebSocketConstants.CLOSE_CODE_ESCALATION,
            WebSocketConstants.CLOSE_REASON_ESCALATION
    );

    public static final CloseStatus DUPLICATE_SESSION = new CloseStatus(
            WebSocketConstants.CLOSE_CODE_DUPLICATE_SESSION,
            WebSocketConstants.CLOSE_REASON_DUPLICATE_SESSION
    );

    public static final CloseStatus SERVER_ERROR = CloseStatus.SERVER_ERROR;
    public static final CloseStatus BAD_DATA = CloseStatus.BAD_DATA;
    public static final CloseStatus NORMAL = CloseStatus.NORMAL;
}
