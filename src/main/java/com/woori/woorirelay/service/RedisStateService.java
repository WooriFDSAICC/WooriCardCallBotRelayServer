/**
 *
 *
 * <pre>
 * <b>Description  : Redis 세션 상태 관리</b>
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

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.RedisHashFields;
import com.woori.woorirelay.constant.RelayConstants;
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.model.FdsFlag;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.model.SessionStatus;
import com.woori.woorirelay.support.SessionRegistryKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisStateService {

    private final StringRedisTemplate redisTemplate;
    private final RelayProperties relayProperties;

    public void createSession(CallDirection direction, String sessionId, String campaignId) {
        SessionState state = SessionState.initial(sessionId, direction, campaignId);
        String key = sessionKey(direction, sessionId);
        redisTemplate.opsForHash().putAll(key, state.toHashFields());
        redisTemplate.expire(key, RelayConstants.REDIS_SESSION_TTL_HOURS, TimeUnit.HOURS);
        log.info("[Redis] Session created registryKey={} status={} fdsFlag={}",
                SessionRegistryKeys.registryKey(direction, sessionId), state.getStatus(), state.getFdsFlag());
    }

    public Optional<SessionState> getSession(CallDirection direction, String sessionId) {
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(sessionKey(direction, sessionId));
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(SessionState.fromHash(sessionId, hash));
    }

    public SessionState updateFromAnalysisResult(
            CallDirection direction,
            String sessionId,
            String event,
            String fdsFlag,
            String sttText
    ) {
        String key = sessionKey(direction, sessionId);
        FdsFlag parsedFlag = FdsFlag.from(fdsFlag);
        Instant now = Instant.now();

        redisTemplate.opsForHash().put(key, RedisHashFields.FDS_FLAG, parsedFlag.name());
        redisTemplate.opsForHash().put(key, RedisHashFields.LAST_EVENT, event != null ? event : "");
        if (sttText != null && !sttText.isBlank()) {
            redisTemplate.opsForHash().put(key, RedisHashFields.LAST_STT_TEXT, sttText);
        }
        redisTemplate.opsForHash().put(key, RedisHashFields.UPDATED_AT, now.toString());
        redisTemplate.expire(key, RelayConstants.REDIS_SESSION_TTL_HOURS, TimeUnit.HOURS);

        return getSession(direction, sessionId).orElse(SessionState.initial(sessionId, direction, null));
    }

    public void markEscalated(CallDirection direction, String sessionId) {
        updateStatus(direction, sessionId, SessionStatus.ESCALATED);
        log.warn("[Redis] Session escalated registryKey={}", SessionRegistryKeys.registryKey(direction, sessionId));
    }

    public void markClosed(CallDirection direction, String sessionId) {
        updateStatus(direction, sessionId, SessionStatus.CLOSED);
        log.info("[Redis] Session closed registryKey={}", SessionRegistryKeys.registryKey(direction, sessionId));
    }

    private void updateStatus(CallDirection direction, String sessionId, SessionStatus status) {
        String key = sessionKey(direction, sessionId);
        Instant now = Instant.now();
        redisTemplate.opsForHash().put(key, RedisHashFields.STATUS, status.name());
        redisTemplate.opsForHash().put(key, RedisHashFields.UPDATED_AT, now.toString());
        redisTemplate.expire(key, RelayConstants.REDIS_SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    private String sessionKey(CallDirection direction, String sessionId) {
        return relayProperties.getRedisSessionKeyPrefix()
                + direction.pathSegment() + ":"
                + sessionId;
    }
}
