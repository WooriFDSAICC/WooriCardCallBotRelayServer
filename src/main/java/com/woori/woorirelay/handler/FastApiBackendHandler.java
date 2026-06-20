package com.woori.woorirelay.handler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.function.Consumer;

/**
 * FastAPI AI Gateway → SpringBoot 방향 WebSocket Text 수신 핸들러.
 * STT / FDS JSON 결과를 VoiceIntermediaryHandler 콜백으로 전달한다.
 */
@Slf4j
public class FastApiBackendHandler extends TextWebSocketHandler {

    private final String sessionId;
    private final Consumer<String> resultConsumer;
    private final Runnable disconnectCallback;

    public FastApiBackendHandler(
            String sessionId,
            Consumer<String> resultConsumer,
            Runnable disconnectCallback
    ) {
        this.sessionId = sessionId;
        this.resultConsumer = resultConsumer;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        // FastAPI는 Text(JSON) 프레임으로 STT/FDS 제어 플래그를 반환
        String payload = message.getPayload();
        log.debug("[FastAPI→Relay] sessionId={} payloadLength={}", sessionId, payload.length());
        resultConsumer.accept(payload);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("[FastAPI→Relay] Backend WebSocket connected sessionId={} wsId={}",
                sessionId, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("[FastAPI→Relay] Backend WebSocket closed sessionId={} status={}",
                sessionId, status);
        disconnectCallback.run();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("[FastAPI→Relay] Transport error sessionId={}", sessionId, exception);
        disconnectCallback.run();
    }
}
