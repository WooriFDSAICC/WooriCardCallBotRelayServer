/**
 *
 *
 * <pre>
 * <b>Description  : 프로세스 종료 시 활성 세션 정리</b>
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
 *  2026.06.22        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.config;

import com.woori.woorirelay.constant.RelayCloseStatus;
import com.woori.woorirelay.registry.VoiceSessionRegistry;
import com.woori.woorirelay.service.VoiceSessionLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 프로세스 종료 시 활성 통화를 정리하고 Kafka SESSION_ENDED를 발행한다.
 * 배포 환경에서 rolling restart·무중단 배포 전에 LB 트래픽 차단과 함께 사용한다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GracefulShutdownHandler {

    private static final String SHUTDOWN_REASON = "Server shutdown";

    private final VoiceSessionRegistry sessionRegistry;
    private final VoiceSessionLifecycleService lifecycleService;

    @EventListener(ContextClosedEvent.class)
    public void onShutdown(ContextClosedEvent event) {
        List<String> registryKeys = new ArrayList<>(sessionRegistry.activeRegistryKeys());
        if (registryKeys.isEmpty()) {
            return;
        }
        log.warn("[Shutdown] Draining {} active session(s)", registryKeys.size());
        for (String registryKey : registryKeys) {
            lifecycleService.terminateSession(
                    registryKey,
                    RelayCloseStatus.SERVER_ERROR,
                    SHUTDOWN_REASON,
                    false
            );
        }
    }
}
