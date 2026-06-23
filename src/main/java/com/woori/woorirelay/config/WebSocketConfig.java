/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket 핸들러 및 컨테이너 설정</b>
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

import com.woori.woorirelay.constant.WebSocketConstants;
import com.woori.woorirelay.handler.VoiceIntermediaryHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final VoiceIntermediaryHandler voiceIntermediaryHandler;
    private final RelayProperties relayProperties;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        String[] allowedOrigins = relayProperties.getWebsocket().getAllowedOrigins().split(",");
        registry.addHandler(voiceIntermediaryHandler, WebSocketConstants.VOICE_INBOUND_PATH)
                .setAllowedOrigins(allowedOrigins);
        registry.addHandler(voiceIntermediaryHandler, WebSocketConstants.VOICE_OUTBOUND_PATH)
                .setAllowedOrigins(allowedOrigins);
        registry.addHandler(voiceIntermediaryHandler, WebSocketConstants.VOICE_STREAM_PATH)
                .setAllowedOrigins(allowedOrigins);
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        RelayProperties.WebSocket ws = relayProperties.getWebsocket();
        container.setMaxTextMessageBufferSize(ws.getMaxTextMessageBufferSize());
        container.setMaxBinaryMessageBufferSize(ws.getMaxBinaryMessageBufferSize());
        return container;
    }
}
