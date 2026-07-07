/**
 *
 *
 * <pre>
 * <b>Description  : TTS Worker 아웃바운드 WebSocket 연결(봇 발화 다운링크)</b>
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
 *  2026.07.07        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.RelayConstants;
import com.woori.woorirelay.handler.TtsWorkerBackendHandler;
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.session.VoiceSessionEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 세션별로 TTS Worker 에 아웃바운드 WS 를 연다. TTS 는 비핵심 경로이므로
 * 연결 실패는 통화를 종료시키지 않고 로깅만 한다(해당 세션은 봇 발화 없이 진행).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsWorkerConnectionService {

    private final RelayProperties relayProperties;
    private final StandardWebSocketClient webSocketClient;

    public boolean isEnabled() {
        return relayProperties.getTtsWorker().isEnabled();
    }

    public void connect(
            VoiceSessionEntry entry,
            Consumer<ByteBuffer> ttsAudioHandler,
            Runnable disconnectHandler
    ) {
        if (!isEnabled()) {
            return;
        }
        String url = buildWorkerUrl(entry.getDirection(), entry.getSessionId());

        TtsWorkerBackendHandler handler = new TtsWorkerBackendHandler(
                entry.getSessionId(), entry.getRegistryKey(), ttsAudioHandler, disconnectHandler);

        try {
            WebSocketSession workerSession = webSocketClient
                    .execute(handler, null, URI.create(url))
                    .get(RelayConstants.TTS_WORKER_CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            entry.bindWorkerSession(workerSession, handler);
            log.info("[TtsWorkerConnection] connected registryKey={} url={}",
                    entry.getRegistryKey(), url);
        } catch (Exception ex) {
            // 비핵심: 봇 발화만 비활성. 분석/에스컬레이션 경로는 정상 동작.
            log.warn("[TtsWorkerConnection] connect failed (TTS disabled for session) registryKey={} url={}: {}",
                    entry.getRegistryKey(), url, ex.toString());
        }
    }

    public String buildWorkerUrl(CallDirection direction, String sessionId) {
        String baseUrl = relayProperties.resolveTtsWorkerWsBaseUrl(direction);
        if (baseUrl.endsWith("/")) {
            return baseUrl + sessionId;
        }
        return baseUrl + "/" + sessionId;
    }
}
