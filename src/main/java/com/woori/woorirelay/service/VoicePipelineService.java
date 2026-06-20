package com.woori.woorirelay.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.woori.woorirelay.constant.RelayCloseStatus;
import com.woori.woorirelay.constant.TerminationReason;
import com.woori.woorirelay.model.FastApiStreamResult;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.config.RelayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 음성 스트리밍 파이프라인 오케스트레이션 서비스.
 *
 * Redis/Kafka/에스컬레이션/연결/라이프사이클은 각 전담 서비스에 위임한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VoicePipelineService {

    private final RedisStateService redisStateService;
    private final KafkaProducerService kafkaProducerService;
    private final VoiceSessionLifecycleService lifecycleService;
    private final FastApiConnectionService fastApiConnectionService;
    private final VoiceSessionRegistry sessionRegistry;
    private final RelayProperties relayProperties;
    private final ObjectMapper objectMapper;

    public void onSessionStarted(String sessionId, VoiceSessionEntry entry) {
        redisStateService.createSession(sessionId);
        fastApiConnectionService.connect(
                entry,
                payload -> processFastApiResult(sessionId, payload),
                () -> onBackendDisconnected(sessionId)
        );
    }

    public boolean forwardAudioChunk(String sessionId, ByteBuffer payload) {
        VoiceSessionEntry entry = sessionRegistry.find(sessionId).orElse(null);
        if (entry == null || !entry.isActive()) {
            return true;
        }

        int remaining = payload.remaining();
        if (remaining == 0) {
            return true;
        }

        int maxChunk = relayProperties.getMaxBinaryChunkBytes();
        if (remaining > maxChunk) {
            log.warn("[Pipeline] Oversized chunk sessionId={} bytes={} max={}",
                    sessionId, remaining, maxChunk);
            lifecycleService.terminateSession(
                    sessionId,
                    RelayCloseStatus.SERVER_ERROR,
                    TerminationReason.BINARY_CHUNK_EXCEEDS_LIMIT,
                    false
            );
            return false;
        }

        WebSocketSession backendSession = entry.getBackendSession();
        if (backendSession == null || !backendSession.isOpen()) {
            log.warn("[Pipeline] Backend unavailable sessionId={}", sessionId);
            return true;
        }

        try {
            backendSession.sendMessage(new BinaryMessage(payload.asReadOnlyBuffer()));
            return true;
        } catch (IOException ex) {
            log.error("[Pipeline] Audio forward failed sessionId={}", sessionId, ex);
            lifecycleService.terminateSession(
                    sessionId,
                    RelayCloseStatus.SERVER_ERROR,
                    TerminationReason.AUDIO_FORWARD_FAILURE,
                    false
            );
            return false;
        }
    }

    public void processFastApiResult(String sessionId, String jsonPayload) {
        VoiceSessionEntry entry = sessionRegistry.find(sessionId).orElse(null);
        if (entry == null || !entry.isActive()) {
            return;
        }

        try {
            FastApiStreamResult result = objectMapper.readValue(jsonPayload, FastApiStreamResult.class);
            FdsEvent event = result.toFdsEvent(sessionId);

            SessionState updatedState = redisStateService.updateFromAnalysisResult(
                    sessionId,
                    result.getEvent(),
                    result.getFdsFlag(),
                    result.getSttText()
            );

            kafkaProducerService.publishFdsEvent(event);

            if (event.requiresEscalation()) {
                lifecycleService.escalateAndClose(entry, updatedState, event);
            }
        } catch (Exception ex) {
            log.error("[Pipeline] FastAPI result processing failed sessionId={} payload={}",
                    sessionId, jsonPayload, ex);
        }
    }

    public void onClientDisconnected(String sessionId, org.springframework.web.socket.CloseStatus status) {
        lifecycleService.cleanupOnClientDisconnect(sessionId, status);
    }

    public void onBackendDisconnected(String sessionId) {
        VoiceSessionEntry entry = sessionRegistry.find(sessionId).orElse(null);
        if (entry == null || !entry.isActive()) {
            return;
        }
        log.warn("[Pipeline] FastAPI backend disconnected sessionId={}", sessionId);
        lifecycleService.terminateSession(
                sessionId,
                RelayCloseStatus.SERVER_ERROR,
                TerminationReason.FASTAPI_BACKEND_DISCONNECTED,
                false
        );
    }
}
