/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket 핸드셰이크 인증 인터셉터</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.config
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.07.07        RosieOh     H1 WebSocket 인증 추가
 *        </pre>
 */

package com.woori.woorirelay.config;

import com.woori.woorirelay.constant.WebSocketConstants;
import com.woori.woorirelay.model.VoiceSessionHandshake;
import com.woori.woorirelay.support.SessionHandshakeExtractor;
import com.woori.woorirelay.support.WebSocketAuthenticator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URI;
import java.util.List;
import java.util.Map;

/**
 * WebSocket 업그레이드 전에 sessionId 바인딩 토큰을 검증한다. 실패 시 401 로 핸드셰이크를 거절해
 * 인증되지 않은 오디오 주입·FDS 이벤트 위조·에스컬레이션 남용을 차단한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WebSocketHandshakeAuthInterceptor implements HandshakeInterceptor {

    private final WebSocketAuthenticator authenticator;
    private final SessionHandshakeExtractor handshakeExtractor;
    private final RelayMetrics relayMetrics;

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        if (!authenticator.isEnabled()) {
            return true;
        }

        URI uri = request.getURI();
        String sessionId = extractSessionId(uri);
        String token = extractToken(request, uri);

        if (sessionId == null || !authenticator.authenticate(sessionId, token)) {
            relayMetrics.recordWsAuthRejection();
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            log.warn("[WsAuth] Handshake rejected uri={} sessionId={}", uri.getPath(), sessionId);
            return false;
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
        // no-op
    }

    private String extractSessionId(URI uri) {
        VoiceSessionHandshake hs = handshakeExtractor.extractFromUri(uri);
        return hs == null ? null : hs.getSessionId();
    }

    /** Authorization: Bearer &lt;token&gt; 헤더 우선, 없으면 ?token= 쿼리 파라미터. */
    private String extractToken(ServerHttpRequest request, URI uri) {
        List<String> authHeaders = request.getHeaders().get(WebSocketConstants.AUTH_HEADER);
        if (authHeaders != null) {
            for (String header : authHeaders) {
                if (header == null || header.isBlank()) {
                    continue;
                }
                if (header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    return header.substring(7).trim();
                }
                return header.trim();
            }
        }
        String query = uri.getQuery();
        if (query != null) {
            for (String param : query.split("&")) {
                String[] pair = param.split("=", 2);
                if (pair.length == 2 && WebSocketConstants.AUTH_QUERY_PARAM.equals(pair[0])) {
                    return pair[1].trim();
                }
            }
        }
        return null;
    }
}
