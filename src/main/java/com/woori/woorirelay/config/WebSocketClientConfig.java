/**
 *
 *
 * <pre>
 * <b>Description  : FastAPI 아웃바운드 WebSocket 클라이언트 설정</b>
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
 *  2026.06.22        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.config;

import jakarta.websocket.ContainerProvider;
import jakarta.websocket.WebSocketContainer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

@Configuration
public class WebSocketClientConfig {

    @Bean
    public StandardWebSocketClient standardWebSocketClient(RelayProperties relayProperties) {
        RelayProperties.WebSocket ws = relayProperties.getWebsocket();
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        container.setDefaultMaxTextMessageBufferSize(ws.getMaxTextMessageBufferSize());
        container.setDefaultMaxBinaryMessageBufferSize(ws.getMaxBinaryMessageBufferSize());
        return new StandardWebSocketClient(container);
    }
}
