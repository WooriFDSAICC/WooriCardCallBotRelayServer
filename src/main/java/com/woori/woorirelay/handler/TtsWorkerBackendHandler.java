/**
 *
 *
 * <pre>
 * <b>Description  : TTS Worker → Relay 방향 WebSocket 수신 핸들러</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.handler
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

package com.woori.woorirelay.handler;

import com.woori.woorirelay.support.WebSocketMdcSupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * TTS Worker → Relay 방향 WebSocket 핸들러.
 * <ul>
 *   <li>Binary: 봇 발화 오디오(8kHz μ-law 프레임) → ttsConsumer(고객에게 재생)</li>
 *   <li>Text: 상태 프레임(ready/speaking_start/end/error) — 로깅</li>
 * </ul>
 * TTS 는 비핵심 경로이므로 워커 연결 장애가 통화를 종료시키지 않는다(disconnect 는 로깅만).
 */
@Slf4j
public class TtsWorkerBackendHandler extends AbstractWebSocketHandler {

    private final String sessionId;
    private final String registryKey;
    private final Consumer<ByteBuffer> ttsConsumer;
    private final Runnable disconnectCallback;

    public TtsWorkerBackendHandler(
            String sessionId,
            String registryKey,
            Consumer<ByteBuffer> ttsConsumer,
            Runnable disconnectCallback
    ) {
        this.sessionId = sessionId;
        this.registryKey = registryKey;
        this.ttsConsumer = ttsConsumer;
        this.disconnectCallback = disconnectCallback;
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                ttsConsumer.accept(message.getPayload()));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                log.debug("[TtsWorker→Relay] status sessionId={} payload={}", sessionId, message.getPayload()));
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                log.info("[TtsWorker→Relay] connected sessionId={} registryKey={} wsId={}",
                        sessionId, registryKey, session.getId()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                log.info("[TtsWorker→Relay] closed sessionId={} registryKey={} status={}",
                        sessionId, registryKey, status));
        disconnectCallback.run();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                log.warn("[TtsWorker→Relay] transport error sessionId={} registryKey={}: {}",
                        sessionId, registryKey, exception.toString()));
    }
}
