package com.woori.woorirelay.registry;

import com.woori.woorirelay.session.VoiceSessionEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 단일 노드(JVM) 내 고객 WebSocket ↔ FastAPI WebSocket 세션 매핑 레지스트리.
 *
 * ConcurrentHashMap 기반으로 다중 통화 동시 접속 시 Thread-safe하게
 * sessionId → VoiceSessionEntry 를 관리한다.
 */
@Slf4j
@Component
public class VoiceSessionRegistry {

    private final ConcurrentHashMap<String, VoiceSessionEntry> sessions = new ConcurrentHashMap<>();

    /**
     * 신규 세션 등록. 동일 sessionId가 이미 존재하면 false 반환(중복 접속 거부).
     */
    public boolean registerIfAbsent(String sessionId, WebSocketSession clientSession) {
        VoiceSessionEntry entry = new VoiceSessionEntry(sessionId, clientSession);
        VoiceSessionEntry previous = sessions.putIfAbsent(sessionId, entry);
        if (previous != null) {
            log.warn("[Registry] Duplicate sessionId rejected sessionId={}", sessionId);
            return false;
        }
        log.debug("[Registry] Session registered sessionId={} clientWsId={}",
                sessionId, clientSession.getId());
        return true;
    }

    public Optional<VoiceSessionEntry> find(String sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    /**
     * 세션 제거 및 제거된 Entry 반환. 없으면 empty.
     */
    public Optional<VoiceSessionEntry> remove(String sessionId) {
        return Optional.ofNullable(sessions.remove(sessionId));
    }

    public boolean contains(String sessionId) {
        return sessions.containsKey(sessionId);
    }

    public int activeSessionCount() {
        return sessions.size();
    }
}
