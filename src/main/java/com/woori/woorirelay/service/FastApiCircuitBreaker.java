/**
 *
 *
 * <pre>
 * <b>Description  : FastAPI 연결 Circuit Breaker(경량, 의존성 없음)</b>
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
 *  2026.07.07        RosieOh     FastAPI 연결 CB 추가
 *        </pre>
 */

package com.woori.woorirelay.service;

import com.woori.woorirelay.config.RelayProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

/**
 * CLOSED → (연속 실패 임계 초과) → OPEN → (cool-down 경과) → HALF_OPEN → (성공) → CLOSED.
 * 게이트웨이 지속 장애 시 연결 시도를 빠르게 차단(fail-fast)해 스레드/커넥션 낭비를 막는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FastApiCircuitBreaker {

    public enum State { CLOSED, OPEN, HALF_OPEN }

    private final RelayProperties relayProperties;
    private final AtomicReference<State> state = new AtomicReference<>(State.CLOSED);
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicLong openedAtMs = new AtomicLong(0);

    /** 연결 시도를 허용할지. OPEN 이고 cool-down 미경과면 false(빠른 실패). */
    public boolean allowRequest() {
        State current = state.get();
        if (current == State.CLOSED || current == State.HALF_OPEN) {
            return true;
        }
        long openMs = relayProperties.getFastApi().getCbOpenMs();
        if (Instant.now().toEpochMilli() - openedAtMs.get() >= openMs) {
            if (state.compareAndSet(State.OPEN, State.HALF_OPEN)) {
                log.info("[FastApiCB] cool-down 경과 → HALF_OPEN(시험 요청 허용)");
            }
            return true;
        }
        return false;
    }

    public void onSuccess() {
        consecutiveFailures.set(0);
        State prev = state.getAndSet(State.CLOSED);
        if (prev != State.CLOSED) {
            log.info("[FastApiCB] 연결 성공 → CLOSED");
        }
    }

    public void onFailure() {
        int failures = consecutiveFailures.incrementAndGet();
        int threshold = relayProperties.getFastApi().getCbFailureThreshold();
        if (state.get() == State.HALF_OPEN || failures >= threshold) {
            if (state.getAndSet(State.OPEN) != State.OPEN) {
                openedAtMs.set(Instant.now().toEpochMilli());
                log.warn("[FastApiCB] 임계 초과(failures={}, threshold={}) → OPEN", failures, threshold);
            }
        }
    }

    public State currentState() {
        return state.get();
    }
}
