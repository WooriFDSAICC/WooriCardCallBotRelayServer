/**
 *
 *
 * <pre>
 * <b>Description  : DistributedSessionOwnershipServiceTest 단위 테스트</b>
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DistributedSessionOwnershipServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private RelayInstanceIdProvider instanceIdProvider;

    private RelayProperties relayProperties;
    private DistributedSessionOwnershipService ownershipService;

    @BeforeEach
    void setUp() {
        relayProperties = new RelayProperties();
        ownershipService = new DistributedSessionOwnershipService(
                redisTemplate,
                relayProperties,
                instanceIdProvider
        );
    }

    @Test
    void tryClaim_whenDisabled_alwaysClaims() {
        SessionOwnershipResult result = ownershipService.tryClaim(CallDirection.INBOUND, "session-1");

        assertTrue(result.isClaimed());
    }

    @Test
    void tryClaim_whenEnabledAndAcquired_claimsOwnership() {
        relayProperties.getDistributedSession().setEnabled(true);
        when(instanceIdProvider.getInstanceId()).thenReturn("relay-a");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(
                eq("wooricard:relay:owner:inbound:session-1"),
                eq("relay-a"),
                eq(60L),
                eq(TimeUnit.SECONDS)
        )).thenReturn(true);

        SessionOwnershipResult result = ownershipService.tryClaim(CallDirection.INBOUND, "session-1");

        assertTrue(result.isClaimed());
    }

    @Test
    void tryClaim_whenOwnedByAnotherLiveInstance_rejects() {
        relayProperties.getDistributedSession().setEnabled(true);
        when(instanceIdProvider.getInstanceId()).thenReturn("relay-a");
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), eq(60L), eq(TimeUnit.SECONDS)))
                .thenReturn(false);
        when(valueOperations.get("wooricard:relay:owner:inbound:session-1")).thenReturn("relay-b");
        when(redisTemplate.hasKey("wooricard:relay:instance:relay-b")).thenReturn(true);

        SessionOwnershipResult result = ownershipService.tryClaim(CallDirection.INBOUND, "session-1");

        assertTrue(result.isRejected());
        assertFalse(result.isClaimed());
    }
}
