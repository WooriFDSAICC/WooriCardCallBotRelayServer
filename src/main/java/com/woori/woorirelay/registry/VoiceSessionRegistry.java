/**
 *
 *
 * <pre>
 * <b>Description  : JVM 로컬 WebSocket 세션 레지스트리</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.registry
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

package com.woori.woorirelay.registry;

import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.support.SessionRegistryKeys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class VoiceSessionRegistry {

    private final ConcurrentHashMap<String, VoiceSessionEntry> sessions = new ConcurrentHashMap<>();

    public boolean registerIfAbsent(VoiceSessionEntry entry) {
        VoiceSessionEntry previous = sessions.putIfAbsent(entry.getRegistryKey(), entry);
        if (previous != null) {
            log.warn("[Registry] Duplicate session rejected registryKey={}", entry.getRegistryKey());
            return false;
        }
        log.debug("[Registry] Session registered registryKey={} clientWsId={}",
                entry.getRegistryKey(), entry.getClientSession().getId());
        return true;
    }

    public Optional<VoiceSessionEntry> find(String registryKey) {
        return Optional.ofNullable(sessions.get(registryKey));
    }

    public Optional<VoiceSessionEntry> find(CallDirection direction, String sessionId) {
        return find(SessionRegistryKeys.registryKey(direction, sessionId));
    }

    public Optional<VoiceSessionEntry> remove(String registryKey) {
        return Optional.ofNullable(sessions.remove(registryKey));
    }

    public Collection<VoiceSessionEntry> activeEntries() {
        return sessions.values();
    }

    public int activeSessionCount() {
        return sessions.size();
    }

    public int activeSessionCount(CallDirection direction) {
        String prefix = direction.pathSegment() + ":";
        return (int) sessions.keySet().stream().filter(key -> key.startsWith(prefix)).count();
    }

    public Set<String> activeRegistryKeys() {
        return Set.copyOf(sessions.keySet());
    }
}
