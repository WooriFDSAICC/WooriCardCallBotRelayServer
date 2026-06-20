package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayProperties;
import com.woori.woorirelay.constant.RedisHashFields;
import com.woori.woorirelay.constant.RelayConstants;
import com.woori.woorirelay.model.FdsFlag;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.model.SessionStatus;
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

    public void createSession(String sessionId) {
        SessionState state = SessionState.initial(sessionId);
        String key = sessionKey(sessionId);
        redisTemplate.opsForHash().putAll(key, state.toHashFields());
        redisTemplate.expire(key, RelayConstants.REDIS_SESSION_TTL_HOURS, TimeUnit.HOURS);
        log.info("[Redis] Session created sessionId={} status={} fdsFlag={}",
                sessionId, state.getStatus(), state.getFdsFlag());
    }

    public Optional<SessionState> getSession(String sessionId) {
        Map<Object, Object> hash = redisTemplate.opsForHash().entries(sessionKey(sessionId));
        if (hash.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(SessionState.fromHash(sessionId, hash));
    }

    public SessionState updateFromAnalysisResult(
            String sessionId,
            String event,
            String fdsFlag,
            String sttText
    ) {
        String key = sessionKey(sessionId);
        FdsFlag parsedFlag = FdsFlag.from(fdsFlag);
        Instant now = Instant.now();

        redisTemplate.opsForHash().put(key, RedisHashFields.FDS_FLAG, parsedFlag.name());
        redisTemplate.opsForHash().put(key, RedisHashFields.LAST_EVENT, event != null ? event : "");
        if (sttText != null && !sttText.isBlank()) {
            redisTemplate.opsForHash().put(key, RedisHashFields.LAST_STT_TEXT, sttText);
        }
        redisTemplate.opsForHash().put(key, RedisHashFields.UPDATED_AT, now.toString());
        redisTemplate.expire(key, RelayConstants.REDIS_SESSION_TTL_HOURS, TimeUnit.HOURS);

        return getSession(sessionId).orElse(SessionState.initial(sessionId));
    }

    public void markEscalated(String sessionId) {
        updateStatus(sessionId, SessionStatus.ESCALATED);
        log.warn("[Redis] Session escalated sessionId={}", sessionId);
    }

    public void markClosed(String sessionId) {
        updateStatus(sessionId, SessionStatus.CLOSED);
        log.info("[Redis] Session closed sessionId={}", sessionId);
    }

    private void updateStatus(String sessionId, SessionStatus status) {
        String key = sessionKey(sessionId);
        Instant now = Instant.now();
        redisTemplate.opsForHash().put(key, RedisHashFields.STATUS, status.name());
        redisTemplate.opsForHash().put(key, RedisHashFields.UPDATED_AT, now.toString());
        redisTemplate.expire(key, RelayConstants.REDIS_SESSION_TTL_HOURS, TimeUnit.HOURS);
    }

    private String sessionKey(String sessionId) {
        return relayProperties.getRedisSessionKeyPrefix() + sessionId;
    }
}
