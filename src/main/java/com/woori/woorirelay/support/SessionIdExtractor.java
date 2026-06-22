/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket URI sessionId 추출 (레거시)</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.support
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

package com.woori.woorirelay.support;

import com.woori.woorirelay.constant.WebSocketConstants;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

/**
 * WebSocket URI 경로에서 sessionId를 추출한다.
 * 예: /voice/abc-123 → abc-123
 */
@Component
public class SessionIdExtractor {

    public String extract(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        int lastSlash = path.lastIndexOf(WebSocketConstants.PATH_SEPARATOR);
        if (lastSlash < 0 || lastSlash == path.length() - 1) {
            return null;
        }
        return path.substring(lastSlash + 1);
    }
}
