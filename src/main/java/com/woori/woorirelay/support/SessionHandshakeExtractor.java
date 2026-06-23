/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket URI handshake 추출</b>
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
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.model.VoiceSessionHandshake;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.Locale;

/**
 * WebSocket URI에서 sessionId·통화방향·campaignId를 추출한다.
 *
 * <ul>
 *   <li>{@code /voice/inbound/{sessionId}} → INBOUND</li>
 *   <li>{@code /voice/outbound/{sessionId}?campaignId=C001} → OUTBOUND</li>
 *   <li>{@code /voice/{sessionId}} → INBOUND (하위 호환)</li>
 * </ul>
 */
@Component
public class SessionHandshakeExtractor {

    public VoiceSessionHandshake extract(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) {
            return null;
        }
        String path = uri.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }

        String[] segments = path.split(WebSocketConstants.PATH_SEPARATOR);
        String sessionId;
        CallDirection direction;

        if (segments.length >= 4 && "voice".equals(segments[1])) {
            String directionSegment = segments[2].toLowerCase(Locale.ROOT);
            if ("inbound".equals(directionSegment)) {
                direction = CallDirection.INBOUND;
            } else if ("outbound".equals(directionSegment)) {
                direction = CallDirection.OUTBOUND;
            } else {
                return null;
            }
            sessionId = segments[3];
        } else if (segments.length >= 3 && "voice".equals(segments[1])) {
            direction = CallDirection.INBOUND;
            sessionId = segments[2];
        } else {
            return null;
        }

        if (sessionId.isBlank()) {
            return null;
        }

        String campaignId = null;
        if (uri.getQuery() != null) {
            for (String param : uri.getQuery().split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && "campaignId".equals(pair[0])) {
                    campaignId = pair[1];
                }
            }
        }

        return VoiceSessionHandshake.builder()
                .sessionId(sessionId)
                .direction(direction)
                .campaignId(campaignId)
                .build();
    }
}
