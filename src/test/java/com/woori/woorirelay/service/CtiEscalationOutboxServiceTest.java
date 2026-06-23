/**
 *
 *
 * <pre>
 * <b>Description  : CtiEscalationOutboxServiceTest 단위 테스트</b>
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
import com.woori.woorirelay.constant.StreamEventTypes;
import com.woori.woorirelay.integration.cti.CtiEscalationResponse;
import com.woori.woorirelay.integration.cti.CtiRoutingClient;
import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.model.FdsEvent;
import com.woori.woorirelay.model.FdsFlag;
import com.woori.woorirelay.model.SessionState;
import com.woori.woorirelay.model.SessionStatus;
import com.woori.woorirelay.session.VoiceSessionEntry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.web.socket.WebSocketSession;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CtiEscalationOutboxServiceTest {

    @Mock
    private CtiRoutingClient ctiRoutingClient;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HashOperations<String, Object, Object> hashOperations;

    @Mock
    private ZSetOperations<String, String> zSetOperations;

    private RelayProperties relayProperties;
    private CtiEscalationOutboxService outboxService;

    @BeforeEach
    void setUp() {
        relayProperties = new RelayProperties();
        outboxService = new CtiEscalationOutboxService(
                ctiRoutingClient,
                redisTemplate,
                relayProperties,
                new SimpleMeterRegistry()
        );
    }

    @Test
    void triggerEscalation_whenAccepted_marksCompleted() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(ctiRoutingClient.enqueueEscalation(any())).thenReturn(
                CtiEscalationResponse.builder().accepted(true).queueId("Q-1").build()
        );

        outboxService.triggerEscalation(sampleEntry(), sampleState(), sampleEvent());

        verify(valueOperations).set(
                eq("wooricard:cti:outbox:done:inbound:session-1"),
                anyString(),
                eq(24L),
                eq(TimeUnit.HOURS)
        );
        verify(zSetOperations).remove("wooricard:cti:outbox:queue", "inbound:session-1");
    }

    @Test
    void triggerEscalation_whenRejected_enqueuesToOutbox() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(redisTemplate.opsForHash()).thenReturn(hashOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(ctiRoutingClient.enqueueEscalation(any())).thenReturn(
                CtiEscalationResponse.builder().accepted(false).message("timeout").build()
        );

        outboxService.triggerEscalation(sampleEntry(), sampleState(), sampleEvent());

        verify(hashOperations).putAll(eq("wooricard:cti:outbox:item:inbound:session-1"), org.mockito.ArgumentMatchers.<java.util.Map<String, String>>any());
        verify(zSetOperations).add(eq("wooricard:cti:outbox:queue"), eq("inbound:session-1"), anyDouble());
    }

    @Test
    void triggerEscalation_whenAlreadyCompleted_skipsCtiCall() {
        when(redisTemplate.hasKey("wooricard:cti:outbox:done:inbound:session-1")).thenReturn(true);

        outboxService.triggerEscalation(sampleEntry(), sampleState(), sampleEvent());

        verify(ctiRoutingClient, never()).enqueueEscalation(any());
    }

    private VoiceSessionEntry sampleEntry() {
        WebSocketSession clientSession = mock(WebSocketSession.class);
        return new VoiceSessionEntry("session-1", CallDirection.INBOUND, null, clientSession);
    }

    private SessionState sampleState() {
        return SessionState.builder()
                .sessionId("session-1")
                .direction(CallDirection.INBOUND)
                .status(SessionStatus.ESCALATED)
                .fdsFlag(FdsFlag.CRITICAL)
                .lastSttText("카드 분실")
                .updatedAt(Instant.now())
                .build();
    }

    private FdsEvent sampleEvent() {
        return FdsEvent.builder()
                .sessionId("session-1")
                .callDirection(CallDirection.INBOUND.name())
                .eventType(StreamEventTypes.AGENT_ESCALATION)
                .reason("FDS critical")
                .build();
    }
}
