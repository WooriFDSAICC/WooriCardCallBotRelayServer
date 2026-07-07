/**
 *
 *
 * <pre>
 * <b>Description  : 음성 스트리밍 파이프라인 오케스트레이션</b>
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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.woori.woorirelay.config.RelayMetrics;
import com.woori.woorirelay.constant.IntegrationContracts;
import com.woori.woorirelay.constant.RelayCloseStatus;
import com.woori.woorirelay.constant.TerminationReason;
import com.woori.woorirelay.model.FastApiStreamResult;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.support.PiiMaskingUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoicePipelineService {

    private final RedisStateService redisStateService;
    private final KafkaProducerService kafkaProducerService;
    private final VoiceSessionLifecycleService lifecycleService;
    private final FastApiConnectionService fastApiConnectionService;
    private final TtsWorkerConnectionService ttsWorkerConnectionService;
    private final VoiceSessionRegistry sessionRegistry;
    private final RelayProperties relayProperties;
    private final RelayMetrics relayMetrics;
    private final ObjectMapper objectMapper;
    @Qualifier("audioForwardExecutor")
    private final ExecutorService audioForwardExecutor;

    public void onSessionStarted(VoiceSessionEntry entry) {
        redisStateService.createSession(entry.getDirection(), entry.getSessionId(), entry.getCampaignId());
        String registryKey = entry.getRegistryKey();
        fastApiConnectionService.connect(
                entry,
                payload -> processFastApiResult(registryKey, payload),
                () -> onBackendDisconnected(registryKey)
        );
        // 봇 발화 다운링크용 TTS Worker 연결(비핵심 — 실패해도 통화 유지).
        ttsWorkerConnectionService.connect(
                entry,
                ttsPayload -> forwardTtsToClient(registryKey, ttsPayload),
                () -> log.info("[Pipeline] TTS worker disconnected registryKey={}", registryKey)
        );
    }

    /**
     * TTS Worker → Relay 다운링크 오디오(봇 발화, 8kHz μ-law 프레임)를
     * 고객/교환기 클라이언트 세션으로 그대로 재생 전달한다.
     * TTS 는 비핵심 경로이므로 전송 실패는 통화를 종료시키지 않고 로깅만 한다.
     */
    public void forwardTtsToClient(String registryKey, ByteBuffer payload) {
        VoiceSessionEntry entry = sessionRegistry.find(registryKey).orElse(null);
        if (entry == null || !entry.isActive()) {
            return;
        }
        if (payload == null || payload.remaining() == 0) {
            return;
        }
        WebSocketSession clientSession = entry.getClientSession();
        if (clientSession == null || !clientSession.isOpen()) {
            return;
        }
        int frameBytes = payload.remaining();
        try {
            clientSession.sendMessage(new BinaryMessage(payload.asReadOnlyBuffer()));
            relayMetrics.recordTtsDownlink(frameBytes);
        } catch (IOException ex) {
            log.warn("[Pipeline] TTS downlink send failed (skipping frame) registryKey={}: {}",
                    registryKey, ex.toString());
        }
    }

    public boolean forwardAudioChunk(String registryKey, ByteBuffer payload) {
        VoiceSessionEntry entry = sessionRegistry.find(registryKey).orElse(null);
        if (entry == null || !entry.isActive()) {
            return true;
        }

        int remaining = payload.remaining();
        if (remaining == 0) {
            return true;
        }

        int maxChunk = relayProperties.getMaxBinaryChunkBytes();
        if (remaining > maxChunk) {
            log.warn("[Pipeline] Oversized chunk registryKey={} bytes={} max={}",
                    registryKey, remaining, maxChunk);
            lifecycleService.terminateSession(
                    registryKey,
                    RelayCloseStatus.SERVER_ERROR,
                    TerminationReason.BINARY_CHUNK_EXCEEDS_LIMIT,
                    false
            );
            return false;
        }

        WebSocketSession backendSession = entry.getBackendSession();
        if (backendSession == null || !backendSession.isOpen()) {
            log.warn("[Pipeline] Backend unavailable registryKey={}", registryKey);
            return true;
        }

        // 백프레셔: WS read 스레드는 큐 적재만. 버퍼는 컨테이너 소유라 반드시 복사한다.
        byte[] chunk = new byte[remaining];
        payload.duplicate().get(chunk);
        if (!entry.offerAudio(chunk, relayProperties.getAudioQueueCapacity())) {
            relayMetrics.recordAudioChunkDropped();
            if (log.isDebugEnabled()) {
                log.debug("[Pipeline] Audio queue full — dropping chunk registryKey={} bytes={}",
                        registryKey, remaining);
            }
            return true; // 드랍하되 통화는 유지
        }
        scheduleDrain(entry);
        return true;
    }

    private void scheduleDrain(VoiceSessionEntry entry) {
        if (entry.getAudioDraining().compareAndSet(false, true)) {
            audioForwardExecutor.execute(() -> drainAudio(entry));
        }
    }

    /** 세션당 단일 드레인 태스크(프레임 순서 보장). blocking send 는 여기서 발생한다. */
    private void drainAudio(VoiceSessionEntry entry) {
        String registryKey = entry.getRegistryKey();
        try {
            byte[] chunk;
            while ((chunk = entry.pollAudio()) != null) {
                if (!entry.isActive()) {
                    entry.clearAudio();
                    return;
                }
                WebSocketSession backend = entry.getBackendSession();
                if (backend == null || !backend.isOpen()) {
                    entry.clearAudio();
                    return;
                }
                try {
                    if (entry.getBackendHandler() != null) {
                        entry.getBackendHandler().markSttWaitStarted();
                    }
                    backend.sendMessage(new BinaryMessage(ByteBuffer.wrap(chunk)));
                } catch (IOException ex) {
                    log.error("[Pipeline] Audio forward failed registryKey={}", registryKey, ex);
                    entry.clearAudio();
                    lifecycleService.terminateSession(
                            registryKey,
                            RelayCloseStatus.SERVER_ERROR,
                            TerminationReason.AUDIO_FORWARD_FAILURE,
                            false
                    );
                    return;
                }
            }
        } finally {
            entry.getAudioDraining().set(false);
            // 드레인 종료와 신규 적재 사이 경합 보정: 남은 게 있으면 재스케줄.
            if (!entry.audioQueueEmpty() && entry.isActive()) {
                scheduleDrain(entry);
            }
        }
    }

    public void processFastApiResult(String registryKey, String jsonPayload) {
        VoiceSessionEntry entry = sessionRegistry.find(registryKey).orElse(null);
        if (entry == null || !entry.isActive()) {
            return;
        }

        try {
            FastApiStreamResult result = objectMapper.readValue(jsonPayload, FastApiStreamResult.class);

            // 봇 발화 제어 이벤트는 FDS 파이프라인을 타지 않고 TTS Worker 로 라우팅한다.
            // (TTS 발화 텍스트는 봇 스크립트이므로 마스킹 대상이 아니며, 이 분기에서 먼저 반환한다.)
            if (IntegrationContracts.EVENT_TTS_SAY.equals(result.getEvent())
                    || IntegrationContracts.EVENT_TTS_STOP.equals(result.getEvent())) {
                routeTtsControl(entry, result);
                return;
            }

            // H2: 고객 STT 원문에는 카드/주민번호가 포함될 수 있으므로 Redis 저장·Kafka 발행 전
            //     반드시 PII 를 레닥션한다. 이후 경로(Redis/Kafka)는 레닥션된 값만 사용한다.
            result.setSttText(PiiMaskingUtil.redactPii(result.getSttText()));

            FdsEvent event = result.toFdsEvent(entry);

            SessionState updatedState = redisStateService.updateFromAnalysisResult(
                    entry.getDirection(),
                    entry.getSessionId(),
                    result.getEvent(),
                    result.getFdsFlag(),
                    result.getSttText()
            );

            kafkaProducerService.publishFdsEvent(event);

            if (event.requiresEscalation()) {
                lifecycleService.escalateAndClose(entry, updatedState, event);
            }
        } catch (Exception ex) {
            log.error("[Pipeline] FastAPI result processing failed registryKey={} payload={}",
                    registryKey, PiiMaskingUtil.maskForLog(jsonPayload), ex);
        }
    }

    /**
     * 봇 발화 제어(TTS_SAY/TTS_STOP)를 TTS Worker WS 로 라우팅한다.
     * Worker 프로토콜: {@code {"op":"say","text":...,"voice"?,...}} / {@code {"op":"stop"}}.
     */
    private void routeTtsControl(VoiceSessionEntry entry, FastApiStreamResult result) {
        WebSocketSession workerSession = entry.getWorkerSession();
        if (workerSession == null || !workerSession.isOpen()) {
            log.debug("[Pipeline] TTS worker unavailable, drop control registryKey={} event={}",
                    entry.getRegistryKey(), result.getEvent());
            return;
        }
        ObjectNode msg = objectMapper.createObjectNode();
        Map<String, Object> md = result.getMetadata();
        if (IntegrationContracts.EVENT_TTS_STOP.equals(result.getEvent())) {
            msg.put("op", "stop");
        } else {
            msg.put("op", "say");
            msg.put("text", result.getSttText() == null ? "" : result.getSttText());
            if (md != null) {
                putIfString(msg, "voice", md.get("tts_voice"));
                putIfString(msg, "emotion", md.get("tts_emotion"));
                putIfString(msg, "lang", md.get("tts_lang"));
                putIfString(msg, "instruct", md.get("tts_instruct"));
                if (md.get("tts_speed") instanceof Number speed) {
                    msg.put("speed", speed.doubleValue());
                }
            }
        }
        try {
            workerSession.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
        } catch (IOException ex) {
            log.warn("[Pipeline] TTS control send failed registryKey={} event={}: {}",
                    entry.getRegistryKey(), result.getEvent(), ex.toString());
        }
    }

    private static void putIfString(ObjectNode node, String key, Object val) {
        if (val instanceof String s && !s.isBlank()) {
            node.put(key, s);
        }
    }

    public void onClientDisconnected(String registryKey, org.springframework.web.socket.CloseStatus status) {
        lifecycleService.cleanupOnClientDisconnect(registryKey, status);
    }

    public void onBackendDisconnected(String registryKey) {
        VoiceSessionEntry entry = sessionRegistry.find(registryKey).orElse(null);
        if (entry == null || !entry.isActive()) {
            return;
        }
        log.warn("[Pipeline] FastAPI backend disconnected registryKey={}", registryKey);
        lifecycleService.terminateSession(
                registryKey,
                RelayCloseStatus.SERVER_ERROR,
                TerminationReason.FASTAPI_BACKEND_DISCONNECTED,
                false
        );
    }
}
