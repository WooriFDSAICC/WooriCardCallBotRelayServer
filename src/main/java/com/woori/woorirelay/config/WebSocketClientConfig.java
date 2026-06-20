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
