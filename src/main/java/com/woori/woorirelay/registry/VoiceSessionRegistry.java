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

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.session.VoiceSessionEntry;
import com.woori.woorirelay.support.SessionRegistryKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceSessionRegistry {

    private final RelayProperties relayProperties;
    private final ConcurrentHashMap<String, VoiceSessionEntry> sessions = new ConcurrentHashMap<>();

    /** 인스턴스당 세션 상한 도달 여부(0 = 무제한). DoS/자원고갈 방지. */
    public boolean hasCapacity() {
        int max = relayProperties.getMaxSessionsPerInstance();
        return max <= 0 || sessions.size() < max;
    }

    public boolean registerIfAbsent(VoiceSessionEntry entry) {
        int max = relayProperties.getMaxSessionsPerInstance();
        if (max > 0 && sessions.size() >= max) {
            log.warn("[Registry] Capacity reached ({}), rejecting registryKey={}", max, entry.getRegistryKey());
            return false;
        }
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
