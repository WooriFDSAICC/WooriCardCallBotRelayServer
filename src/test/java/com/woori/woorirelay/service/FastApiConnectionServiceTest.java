/**
 *
 *
 * <pre>
 * <b>Description  : FastApiConnectionServiceTest 단위 테스트</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.service
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

package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.session.VoiceSessionEntry;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class FastApiConnectionServiceTest {

    @Test
    void buildBackendUrl_usesDirectionSpecificBaseUrl() {
        RelayProperties properties = new RelayProperties();
        properties.setFastApiWsInboundBaseUrl("ws://gateway/inbound");
        properties.setFastApiWsOutboundBaseUrl("ws://gateway/outbound");

        FastApiConnectionService service = new FastApiConnectionService(
                properties,
                null,
                null,
                null
        );

        WebSocketSession clientSession = mock(WebSocketSession.class);
        VoiceSessionEntry inbound = new VoiceSessionEntry("s1", CallDirection.INBOUND, null, clientSession);
        VoiceSessionEntry outbound = new VoiceSessionEntry("s2", CallDirection.OUTBOUND, "C1", clientSession);

        assertEquals("ws://gateway/inbound/s1", service.buildBackendUrl(inbound));
        assertEquals("ws://gateway/outbound/s2", service.buildBackendUrl(outbound));
    }
}
