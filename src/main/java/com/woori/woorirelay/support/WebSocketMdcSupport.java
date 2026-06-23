/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket 스레드 MDC sessionId 주입</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.support
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

package com.woori.woorirelay.support;

import lombok.experimental.UtilityClass;
import org.slf4j.MDC;

/**
 * WebSocket 핸들러 스레드에 sessionId/registryKey를 MDC로 주입한다.
 */
@UtilityClass
public final class WebSocketMdcSupport {

    private static final String SESSION_ID = "sessionId";
    private static final String REGISTRY_KEY = "registryKey";

    public static void runWithSessionId(String sessionId, Runnable action) {
        runWithContext(sessionId, null, action);
    }

    public static void runWithContext(String sessionId, String registryKey, Runnable action) {
        if (sessionId != null && !sessionId.isBlank()) {
            MDC.put(SESSION_ID, sessionId);
        }
        if (registryKey != null && !registryKey.isBlank()) {
            MDC.put(REGISTRY_KEY, registryKey);
        }
        try {
            action.run();
        } finally {
            MDC.remove(SESSION_ID);
            MDC.remove(REGISTRY_KEY);
        }
    }
}
