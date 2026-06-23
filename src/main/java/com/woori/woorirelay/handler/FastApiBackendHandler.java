/**
 *
 *
 * <pre>
 * <b>Description  : FastAPI Gateway JSON 수신 핸들러</b>
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
 *  2026.06.22        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.handler;

import com.woori.woorirelay.config.RelayMetrics;
import com.woori.woorirelay.support.WebSocketMdcSupport;
import io.micrometer.core.instrument.Timer;
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
    private final String registryKey;
    private final Consumer<String> resultConsumer;
    private final Runnable disconnectCallback;
    private final RelayMetrics relayMetrics;
    private Timer.Sample sttResponseSample;

    public FastApiBackendHandler(
            String sessionId,
            String registryKey,
            Consumer<String> resultConsumer,
            Runnable disconnectCallback,
            RelayMetrics relayMetrics
    ) {
        this.sessionId = sessionId;
        this.registryKey = registryKey;
        this.resultConsumer = resultConsumer;
        this.disconnectCallback = disconnectCallback;
        this.relayMetrics = relayMetrics;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () -> {
            if (sttResponseSample != null) {
                sttResponseSample.stop(relayMetrics.getGatewaySttResponseSeconds());
                sttResponseSample = null;
            }
            String payload = message.getPayload();
            if (payload.contains("AGENT_ESCALATION") || payload.contains("\"fds_flag\":\"CRITICAL\"")) {
                relayMetrics.recordGatewayEscalation();
            }
            resultConsumer.accept(payload);
        });
    }

    public void markSttWaitStarted() {
        sttResponseSample = relayMetrics.startGatewaySttResponseTimer();
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                log.info("[FastAPI→Relay] Backend WebSocket connected sessionId={} registryKey={} wsId={}",
                        sessionId, registryKey, session.getId()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                log.info("[FastAPI→Relay] Backend WebSocket closed sessionId={} registryKey={} status={}",
                        sessionId, registryKey, status));
        disconnectCallback.run();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        WebSocketMdcSupport.runWithContext(sessionId, registryKey, () ->
                log.error("[FastAPI→Relay] Transport error sessionId={} registryKey={}",
                        sessionId, registryKey, exception));
        disconnectCallback.run();
    }
}
