/**
 *
 *
 * <pre>
 * <b>Description  : VoiceSessionRegistry 세션 상한 테스트(P3)</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.registry
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

package com.woori.woorirelay.registry;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.session.VoiceSessionEntry;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoiceSessionRegistryCapacityTest {

    private VoiceSessionEntry entry(String sessionId, CallDirection direction) {
        WebSocketSession ws = mock(WebSocketSession.class);
        when(ws.getId()).thenReturn("ws-" + sessionId);
        return new VoiceSessionEntry(sessionId, direction, null, ws);
    }

    @Test
    void rejectsWhenCapacityReached() {
        RelayProperties props = new RelayProperties();
        props.setMaxSessionsPerInstance(2);
        VoiceSessionRegistry registry = new VoiceSessionRegistry(props);

        assertTrue(registry.hasCapacity());
        assertTrue(registry.registerIfAbsent(entry("s1", CallDirection.INBOUND)));
        assertTrue(registry.registerIfAbsent(entry("s2", CallDirection.INBOUND)));
        assertFalse(registry.hasCapacity());
        assertFalse(registry.registerIfAbsent(entry("s3", CallDirection.INBOUND)));
    }

    @Test
    void unlimitedWhenZero() {
        RelayProperties props = new RelayProperties();
        props.setMaxSessionsPerInstance(0);
        VoiceSessionRegistry registry = new VoiceSessionRegistry(props);
        for (int i = 0; i < 100; i++) {
            assertTrue(registry.registerIfAbsent(entry("s" + i, CallDirection.INBOUND)));
        }
        assertTrue(registry.hasCapacity());
    }
}
