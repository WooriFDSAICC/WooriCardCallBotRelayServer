/**
 *
 *
 * <pre>
 * <b>Description  : WebSocketAuthenticator 단위 테스트(H1)</b>
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
 *  2026.07.07        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.support;

import com.woori.woorirelay.config.RelayProperties;
import org.junit.jupiter.api.Test;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WebSocketAuthenticatorTest {

    private static final String SECRET = "test-secret-key-0123456789";
    private static final String SESSION_ID = "sess-abc-001";

    private RelayProperties hmacProps() {
        RelayProperties props = new RelayProperties();
        RelayProperties.Auth auth = props.getWebsocket().getAuth();
        auth.setEnabled(true);
        auth.setMode(RelayProperties.Auth.Mode.HMAC);
        auth.setSecret(SECRET);
        auth.setClockSkewSeconds(30);
        return props;
    }

    private static String hmacToken(String sessionId, long expiry, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal((sessionId + "." + expiry).getBytes(StandardCharsets.UTF_8));
            return expiry + "." + HexFormat.of().formatHex(raw);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void hmac_validToken_authenticates() {
        WebSocketAuthenticator auth = new WebSocketAuthenticator(hmacProps());
        long expiry = Instant.now().getEpochSecond() + 60;
        assertTrue(auth.authenticate(SESSION_ID, hmacToken(SESSION_ID, expiry, SECRET)));
    }

    @Test
    void hmac_expiredToken_rejected() {
        WebSocketAuthenticator auth = new WebSocketAuthenticator(hmacProps());
        long expiry = Instant.now().getEpochSecond() - 120;
        assertFalse(auth.authenticate(SESSION_ID, hmacToken(SESSION_ID, expiry, SECRET)));
    }

    @Test
    void hmac_tokenBoundToDifferentSession_rejected() {
        WebSocketAuthenticator auth = new WebSocketAuthenticator(hmacProps());
        long expiry = Instant.now().getEpochSecond() + 60;
        String token = hmacToken("other-session", expiry, SECRET);
        assertFalse(auth.authenticate(SESSION_ID, token));
    }

    @Test
    void hmac_wrongSecret_rejected() {
        WebSocketAuthenticator auth = new WebSocketAuthenticator(hmacProps());
        long expiry = Instant.now().getEpochSecond() + 60;
        assertFalse(auth.authenticate(SESSION_ID, hmacToken(SESSION_ID, expiry, "wrong-secret")));
    }

    @Test
    void hmac_malformedToken_rejected() {
        WebSocketAuthenticator auth = new WebSocketAuthenticator(hmacProps());
        assertFalse(auth.authenticate(SESSION_ID, "not-a-valid-token"));
        assertFalse(auth.authenticate(SESSION_ID, ""));
        assertFalse(auth.authenticate(SESSION_ID, null));
    }

    @Test
    void staticMode_matchesConfiguredToken() {
        RelayProperties props = new RelayProperties();
        RelayProperties.Auth auth = props.getWebsocket().getAuth();
        auth.setEnabled(true);
        auth.setMode(RelayProperties.Auth.Mode.STATIC);
        auth.setStaticTokens("tok-a, tok-b , tok-c");
        WebSocketAuthenticator authenticator = new WebSocketAuthenticator(props);
        assertTrue(authenticator.authenticate(SESSION_ID, "tok-b"));
        assertFalse(authenticator.authenticate(SESSION_ID, "tok-x"));
    }

    @Test
    void disabled_alwaysPasses() {
        RelayProperties props = new RelayProperties();
        props.getWebsocket().getAuth().setEnabled(false);
        WebSocketAuthenticator authenticator = new WebSocketAuthenticator(props);
        assertTrue(authenticator.authenticate(SESSION_ID, null));
    }
}
