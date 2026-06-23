/**
 *
 *
 * <pre>
 * <b>Description  : SessionHandshakeExtractorTest 단위 테스트</b>
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

import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.model.VoiceSessionHandshake;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionHandshakeExtractorTest {

    private final SessionHandshakeExtractor extractor = new SessionHandshakeExtractor();

    @Test
    void extract_inboundPath() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(new URI("ws://localhost:8080/voice/inbound/call-001"));

        VoiceSessionHandshake handshake = extractor.extract(session);

        assertNotNull(handshake);
        assertEquals("call-001", handshake.getSessionId());
        assertEquals(CallDirection.INBOUND, handshake.getDirection());
        assertEquals("inbound:call-001", handshake.getRegistryKey());
    }

    @Test
    void extract_outboundPathWithCampaign() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(new URI("ws://localhost:8080/voice/outbound/call-002?campaignId=CAMP01"));

        VoiceSessionHandshake handshake = extractor.extract(session);

        assertNotNull(handshake);
        assertEquals("call-002", handshake.getSessionId());
        assertEquals(CallDirection.OUTBOUND, handshake.getDirection());
        assertEquals("CAMP01", handshake.getCampaignId());
    }

    @Test
    void extract_legacyPathDefaultsInbound() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(new URI("ws://localhost:8080/voice/call-003"));

        VoiceSessionHandshake handshake = extractor.extract(session);

        assertNotNull(handshake);
        assertEquals(CallDirection.INBOUND, handshake.getDirection());
    }

    @Test
    void extract_invalidPathReturnsNull() throws Exception {
        WebSocketSession session = mock(WebSocketSession.class);
        when(session.getUri()).thenReturn(new URI("ws://localhost:8080/health"));

        assertNull(extractor.extract(session));
    }
}
