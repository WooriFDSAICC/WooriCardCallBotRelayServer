/**
 *
 *
 * <pre>
 * <b>Description  : FastApiCircuitBreaker 단위 테스트(P4)</b>
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
 *  2026.07.07        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FastApiCircuitBreakerTest {

    private RelayProperties props(int threshold, long openMs) {
        RelayProperties p = new RelayProperties();
        p.getFastApi().setCbFailureThreshold(threshold);
        p.getFastApi().setCbOpenMs(openMs);
        return p;
    }

    @Test
    void opensAfterConsecutiveFailures() {
        FastApiCircuitBreaker cb = new FastApiCircuitBreaker(props(3, 10_000));
        assertTrue(cb.allowRequest());
        cb.onFailure();
        cb.onFailure();
        assertEquals(FastApiCircuitBreaker.State.CLOSED, cb.currentState());
        cb.onFailure(); // 3번째 → OPEN
        assertEquals(FastApiCircuitBreaker.State.OPEN, cb.currentState());
        assertFalse(cb.allowRequest());
    }

    @Test
    void successResetsFailureCount() {
        FastApiCircuitBreaker cb = new FastApiCircuitBreaker(props(3, 10_000));
        cb.onFailure();
        cb.onFailure();
        cb.onSuccess();
        cb.onFailure();
        cb.onFailure();
        assertEquals(FastApiCircuitBreaker.State.CLOSED, cb.currentState());
    }

    @Test
    void halfOpenAfterCooldown_thenSuccessCloses() {
        FastApiCircuitBreaker cb = new FastApiCircuitBreaker(props(1, 0)); // 즉시 cool-down
        cb.onFailure(); // OPEN
        assertEquals(FastApiCircuitBreaker.State.OPEN, cb.currentState());
        assertTrue(cb.allowRequest()); // cool-down 0 → HALF_OPEN 전환 허용
        assertEquals(FastApiCircuitBreaker.State.HALF_OPEN, cb.currentState());
        cb.onSuccess();
        assertEquals(FastApiCircuitBreaker.State.CLOSED, cb.currentState());
    }

    @Test
    void halfOpenFailure_reopens() {
        FastApiCircuitBreaker cb = new FastApiCircuitBreaker(props(1, 0));
        cb.onFailure();
        cb.allowRequest(); // → HALF_OPEN
        cb.onFailure();    // HALF_OPEN 실패 → 재OPEN
        assertEquals(FastApiCircuitBreaker.State.OPEN, cb.currentState());
    }
}
