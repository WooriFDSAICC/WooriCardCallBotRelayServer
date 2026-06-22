/**
 *
 *
 * <pre>
 * <b>Description  : Redis 분산 세션 소유권 PoC</b>
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
import com.woori.woorirelay.constant.RelayRedisKeys;
import com.woori.woorirelay.model.CallDirection;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class DistributedSessionOwnershipService {

    private final StringRedisTemplate redisTemplate;
    private final RelayProperties relayProperties;
    private final RelayInstanceIdProvider instanceIdProvider;

    public SessionOwnershipResult tryClaim(CallDirection direction, String sessionId) {
        RelayProperties.DistributedSession config = relayProperties.getDistributedSession();
        if (!config.isEnabled()) {
            return SessionOwnershipResult.claimed();
        }

        String ownerKey = ownerKey(direction, sessionId);
        String instanceId = instanceIdProvider.getInstanceId();
        long ttlSeconds = config.getOwnerTtlSeconds();

        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(ownerKey, instanceId, ttlSeconds, TimeUnit.SECONDS);
        if (Boolean.TRUE.equals(acquired)) {
            refreshInstanceHeartbeat();
            log.debug("[SessionOwner] Claimed direction={} sessionId={} instanceId={}",
                    direction, sessionId, instanceId);
            return SessionOwnershipResult.claimed();
        }

        String currentOwner = redisTemplate.opsForValue().get(ownerKey);
        if (instanceId.equals(currentOwner)) {
            redisTemplate.expire(ownerKey, ttlSeconds, TimeUnit.SECONDS);
            return SessionOwnershipResult.claimed();
        }

        if (currentOwner != null && isInstanceAlive(currentOwner)) {
            log.warn("[SessionOwner] Rejected direction={} sessionId={} owner={}",
                    direction, sessionId, currentOwner);
            return SessionOwnershipResult.rejected(currentOwner);
        }

        redisTemplate.opsForValue().set(ownerKey, instanceId, ttlSeconds, TimeUnit.SECONDS);
        refreshInstanceHeartbeat();
        log.warn("[SessionOwner] Reclaimed stale direction={} sessionId={} previousOwner={} instanceId={}",
                direction, sessionId, currentOwner, instanceId);
        return SessionOwnershipResult.claimed();
    }

    public void refreshOwnership(CallDirection direction, String sessionId) {
        if (!relayProperties.getDistributedSession().isEnabled()) {
            return;
        }
        String ownerKey = ownerKey(direction, sessionId);
        String instanceId = instanceIdProvider.getInstanceId();
        String currentOwner = redisTemplate.opsForValue().get(ownerKey);
        if (instanceId.equals(currentOwner)) {
            redisTemplate.expire(ownerKey, relayProperties.getDistributedSession().getOwnerTtlSeconds(), TimeUnit.SECONDS);
        }
    }

    public void release(CallDirection direction, String sessionId) {
        if (!relayProperties.getDistributedSession().isEnabled()) {
            return;
        }
        String ownerKey = ownerKey(direction, sessionId);
        String instanceId = instanceIdProvider.getInstanceId();
        String currentOwner = redisTemplate.opsForValue().get(ownerKey);
        if (instanceId.equals(currentOwner)) {
            redisTemplate.delete(ownerKey);
            log.debug("[SessionOwner] Released direction={} sessionId={} instanceId={}",
                    direction, sessionId, instanceId);
        }
    }

    public void refreshInstanceHeartbeat() {
        if (!relayProperties.getDistributedSession().isEnabled()) {
            return;
        }
        RelayProperties.DistributedSession config = relayProperties.getDistributedSession();
        String instanceKey = instanceKey(instanceIdProvider.getInstanceId());
        redisTemplate.opsForValue().set(
                instanceKey,
                Instant.now().toString(),
                config.getOwnerTtlSeconds(),
                TimeUnit.SECONDS
        );
    }

    public boolean isInstanceAlive(String instanceId) {
        if (instanceId == null || instanceId.isBlank()) {
            return false;
        }
        return Boolean.TRUE.equals(redisTemplate.hasKey(instanceKey(instanceId)));
    }

    private String ownerKey(CallDirection direction, String sessionId) {
        return relayProperties.getDistributedSession().getRedisOwnerKeyPrefix()
                + direction.pathSegment() + ":"
                + sessionId;
    }

    private String instanceKey(String instanceId) {
        return relayProperties.getDistributedSession().getRedisInstanceKeyPrefix()
                + RelayRedisKeys.RELAY_INSTANCE_SUFFIX + instanceId;
    }
}
