/**
 *
 *
 * <pre>
 * <b>Description  : Relay 전용 스레드풀(오디오 백프레셔·CTI 비동기)</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.config
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.07.07        RosieOh     백프레셔/CTI 비동기 executor 추가
 *        </pre>
 */

package com.woori.woorirelay.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WS read 스레드에서 blocking 작업(오디오 send, CTI HTTP)을 떼어내 head-of-line 블로킹과
 * 스레드 고갈을 방지한다.
 */
@Slf4j
@Configuration
public class RelayExecutorConfig {

    /** 업링크 오디오 드레인 전용. 세션당 단일 드레인이라 CPU 코어 수 기준으로 넉넉히 잡는다. */
    @Bean(name = "audioForwardExecutor", destroyMethod = "shutdown")
    public ExecutorService audioForwardExecutor() {
        int cores = Math.max(2, Runtime.getRuntime().availableProcessors());
        return newPool("relay-audio-", cores, cores * 2, 4096);
    }

    /** CTI 에스컬레이션 HTTP 호출 전용(세션 종료 스레드를 블로킹하지 않도록 분리). */
    @Bean(name = "ctiEscalationExecutor", destroyMethod = "shutdown")
    public ExecutorService ctiEscalationExecutor() {
        return newPool("relay-cti-", 2, 8, 1024);
    }

    private ExecutorService newPool(String prefix, int core, int max, int queueCapacity) {
        AtomicInteger idx = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                core, max, 60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(queueCapacity),
                r -> {
                    Thread t = new Thread(r, prefix + idx.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.CallerRunsPolicy());
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
