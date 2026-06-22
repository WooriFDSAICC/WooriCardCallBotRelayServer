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
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.nio.ByteBuffer;

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

    public void onSessionStarted(VoiceSessionEntry entry) {
        redisStateService.createSession(entry.getDirection(), entry.getSessionId(), entry.getCampaignId());
        String registryKey = entry.getRegistryKey();
        fastApiConnectionService.connect(
                entry,
                payload -> processFastApiResult(registryKey, payload),
                () -> onBackendDisconnected(registryKey)
        );
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

        try {
            if (entry.getBackendHandler() != null) {
                entry.getBackendHandler().markSttWaitStarted();
            }
            backendSession.sendMessage(new BinaryMessage(payload.asReadOnlyBuffer()));
            return true;
        } catch (IOException ex) {
            log.error("[Pipeline] Audio forward failed registryKey={}", registryKey, ex);
            lifecycleService.terminateSession(
                    registryKey,
                    RelayCloseStatus.SERVER_ERROR,
                    TerminationReason.AUDIO_FORWARD_FAILURE,
                    false
            );
            return false;
        }
    }

    public void processFastApiResult(String registryKey, String jsonPayload) {
        VoiceSessionEntry entry = sessionRegistry.find(registryKey).orElse(null);
        if (entry == null || !entry.isActive()) {
            return;
        }

        try {
            FastApiStreamResult result = objectMapper.readValue(jsonPayload, FastApiStreamResult.class);
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
