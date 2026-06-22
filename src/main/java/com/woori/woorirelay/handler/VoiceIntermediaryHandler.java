/**
 *
 *
 * <pre>
 * <b>Description  : 고객/교환기 인입 WebSocket 핸들러</b>
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

import com.woori.woorirelay.constant.RelayCloseStatus;
import com.woori.woorirelay.constant.WebSocketConstants;
import com.woori.woorirelay.model.VoiceSessionHandshake;
import com.woori.woorirelay.registry.DistributedSessionOwnershipService;
import com.woori.woorirelay.registry.SessionOwnershipResult;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.service.VoicePipelineService;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.support.SessionHandshakeExtractor;
import com.woori.woorirelay.support.WebSocketMdcSupport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceIntermediaryHandler extends BinaryWebSocketHandler {

    private final VoiceSessionRegistry sessionRegistry;
    private final VoicePipelineService pipelineService;
    private final SessionHandshakeExtractor handshakeExtractor;
    private final DistributedSessionOwnershipService ownershipService;

    @Override
    public void afterConnectionEstablished(WebSocketSession clientSession) throws Exception {
        VoiceSessionHandshake handshake = handshakeExtractor.extract(clientSession);
        if (handshake == null) {
            log.warn("[Handler] Invalid handshake, closing wsId={}", clientSession.getId());
            clientSession.close(RelayCloseStatus.BAD_DATA);
            return;
        }

        String registryKey = handshake.getRegistryKey();
        SessionOwnershipResult ownership = ownershipService.tryClaim(
                handshake.getDirection(), handshake.getSessionId());
        if (ownership.isRejected()) {
            log.warn("[Handler] Session owned by another instance registryKey={} owner={}",
                    registryKey, ownership.getOwnerInstanceId());
            clientSession.close(RelayCloseStatus.DUPLICATE_SESSION);
            return;
        }

        VoiceSessionEntry entry = new VoiceSessionEntry(
                handshake.getSessionId(),
                handshake.getDirection(),
                handshake.getCampaignId(),
                clientSession
        );

        if (!sessionRegistry.registerIfAbsent(entry)) {
            ownershipService.release(handshake.getDirection(), handshake.getSessionId());
            clientSession.close(RelayCloseStatus.DUPLICATE_SESSION);
            return;
        }

        bindSessionAttributes(clientSession, handshake, registryKey);
        pipelineService.onSessionStarted(entry);
        log.info("[Handler] Client connected registryKey={} direction={} clientWsId={}",
                registryKey, handshake.getDirection(), clientSession.getId());
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession clientSession, BinaryMessage message) {
        String registryKey = (String) clientSession.getAttributes().get(WebSocketConstants.REGISTRY_KEY_ATTRIBUTE);
        if (registryKey == null) {
            return;
        }
        WebSocketMdcSupport.runWithSessionId(registryKey, () ->
                pipelineService.forwardAudioChunk(registryKey, message.getPayload()));
    }

    @Override
    public void afterConnectionClosed(WebSocketSession clientSession, org.springframework.web.socket.CloseStatus status) {
        String registryKey = (String) clientSession.getAttributes().get(WebSocketConstants.REGISTRY_KEY_ATTRIBUTE);
        if (registryKey != null) {
            WebSocketMdcSupport.runWithSessionId(registryKey, () ->
                    pipelineService.onClientDisconnected(registryKey, status));
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        String registryKey = (String) session.getAttributes().get(WebSocketConstants.REGISTRY_KEY_ATTRIBUTE);
        WebSocketMdcSupport.runWithSessionId(registryKey, () ->
                log.error("[Handler] Transport error registryKey={}", registryKey, exception));
        if (registryKey != null) {
            WebSocketMdcSupport.runWithSessionId(registryKey, () ->
                    pipelineService.onClientDisconnected(registryKey, RelayCloseStatus.SERVER_ERROR));
        }
    }

    private void bindSessionAttributes(WebSocketSession clientSession, VoiceSessionHandshake handshake, String registryKey) {
        clientSession.getAttributes().put(WebSocketConstants.SESSION_ID_ATTRIBUTE, handshake.getSessionId());
        clientSession.getAttributes().put(WebSocketConstants.REGISTRY_KEY_ATTRIBUTE, registryKey);
        clientSession.getAttributes().put(WebSocketConstants.CALL_DIRECTION_ATTRIBUTE, handshake.getDirection().name());
        if (handshake.getCampaignId() != null) {
            clientSession.getAttributes().put(WebSocketConstants.CAMPAIGN_ID_ATTRIBUTE, handshake.getCampaignId());
        }
    }
}
