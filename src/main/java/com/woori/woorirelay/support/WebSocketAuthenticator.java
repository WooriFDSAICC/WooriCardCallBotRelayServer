/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket 핸드셰이크 토큰 검증기</b>
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
 *  2026.07.07        RosieOh     H1 WebSocket 인증 추가
 *        </pre>
 */

package com.woori.woorirelay.support;

import com.woori.woorirelay.config.RelayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 핸드셰이크 토큰을 검증한다. sessionId 에 바인딩된 HMAC 토큰(재생 불가) 또는
 * 사전 공유 STATIC 토큰을 지원한다. 순수 로직이라 단위 테스트가 용이하다.
 */
@Slf4j
@Component
public class WebSocketAuthenticator {

    private final RelayProperties relayProperties;
    private final Set<String> staticTokens;

    public WebSocketAuthenticator(RelayProperties relayProperties) {
        this.relayProperties = relayProperties;
        this.staticTokens = Arrays.stream(
                        relayProperties.getWebsocket().getAuth().getStaticTokens().split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** 인증 비활성 시 항상 통과. */
    public boolean isEnabled() {
        return relayProperties.getWebsocket().getAuth().isEnabled();
    }

    /**
     * @param sessionId 핸드셰이크 경로에서 추출한 세션 ID(HMAC 바인딩 대상)
     * @param token     Authorization 헤더 또는 token 쿼리에서 추출한 원시 토큰(Bearer 프리픽스 제거된 값)
     * @return 유효하면 true
     */
    public boolean authenticate(String sessionId, String token) {
        RelayProperties.Auth auth = relayProperties.getWebsocket().getAuth();
        if (!auth.isEnabled()) {
            return true;
        }
        if (token == null || token.isBlank() || sessionId == null || sessionId.isBlank()) {
            return false;
        }
        return switch (auth.getMode()) {
            case STATIC -> staticTokens.contains(token);
            case HMAC -> verifyHmac(sessionId, token, auth);
        };
    }

    /** 토큰 형식: {@code <expiryEpochSec>.<hexHmacSha256(secret, sessionId + "." + expiryEpochSec)>} */
    private boolean verifyHmac(String sessionId, String token, RelayProperties.Auth auth) {
        int dot = token.indexOf('.');
        if (dot <= 0 || dot == token.length() - 1) {
            return false;
        }
        String expiryPart = token.substring(0, dot);
        String signaturePart = token.substring(dot + 1);

        long expiry;
        try {
            expiry = Long.parseLong(expiryPart);
        } catch (NumberFormatException ex) {
            return false;
        }
        long now = Instant.now().getEpochSecond();
        if (now > expiry + auth.getClockSkewSeconds()) {
            log.debug("[WsAuth] Token expired sessionId={} expiry={} now={}", sessionId, expiry, now);
            return false;
        }

        String expected = hmacSha256Hex(auth.getSecret(), sessionId + "." + expiryPart);
        if (expected == null) {
            return false;
        }
        return constantTimeEquals(expected, signaturePart.toLowerCase());
    }

    private static String hmacSha256Hex(String secret, String message) {
        if (secret == null || secret.isBlank()) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            log.error("[WsAuth] HMAC computation failed", ex);
            return null;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
