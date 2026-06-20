package com.woori.woorirelay.session;

import lombok.Getter;
import org.springframework.web.socket.WebSocketSession;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 단일 통화 세션의 양방향 WebSocket 핸들 및 라이프사이클 멱등성 플래그.
 * VoiceSessionRegistry의 value 객체로 사용된다.
 */
@Getter
public class VoiceSessionEntry {

    private final String sessionId;
    private final WebSocketSession clientSession;
    private volatile WebSocketSession backendSession;
    private final AtomicBoolean escalated = new AtomicBoolean(false);
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final Object lifecycleLock = new Object();

    public VoiceSessionEntry(String sessionId, WebSocketSession clientSession) {
        this.sessionId = sessionId;
        this.clientSession = clientSession;
    }

    public void bindBackendSession(WebSocketSession backendSession) {
        this.backendSession = backendSession;
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
