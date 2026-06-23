/**
 *
 *
 * <pre>
 * <b>Description  : 양방향 WebSocket 세션 엔트리</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.session
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

package com.woori.woorirelay.session;

import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.support.SessionRegistryKeys;
import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 단일 통화 세션의 양방향 WebSocket 핸들 및 라이프사이클 멱등성 플래그.
 */
@Getter
public class VoiceSessionEntry {

    private final String registryKey;
    private final String sessionId;
    private final CallDirection direction;
    private final String campaignId;
    private final WebSocketSession clientSession;
    private volatile WebSocketSession backendSession;
    private volatile com.woori.woorirelay.handler.FastApiBackendHandler backendHandler;
    private final AtomicBoolean escalated = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();

    public VoiceSessionEntry(
            String sessionId,
            CallDirection direction,
            String campaignId,
            WebSocketSession clientSession
    ) {
        this.sessionId = sessionId;
        this.direction = direction;
        this.campaignId = campaignId;
        this.registryKey = SessionRegistryKeys.registryKey(direction, sessionId);
        this.clientSession = clientSession;
    }

    public void bindBackendSession(
            WebSocketSession backendSession,
            com.woori.woorirelay.handler.FastApiBackendHandler backendHandler
    ) {
        this.backendSession = backendSession;
        this.backendHandler = backendHandler;
    }

    public boolean isActive() {
        return !closed.get();
    }

    public boolean markEscalatedOnce() {
        return escalated.compareAndSet(false, true);
    }

    public boolean markClosedOnce() {
        return closed.compareAndSet(false, true);
    }
}
