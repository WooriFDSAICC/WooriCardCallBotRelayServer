/**
 *
 *
 * <pre>
 * <b>Description  : 세션 registryKey 생성 유틸리티</b>
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

import com.woori.woorirelay.model.CallDirection;
import com.woori.woorirelay.model.VoiceSessionHandshake;
import lombok.experimental.UtilityClass;

@UtilityClass
public final class SessionRegistryKeys {

    public static String registryKey(CallDirection direction, String sessionId) {
        return direction.pathSegment() + ":" + sessionId;
    }

    public static String registryKey(VoiceSessionHandshake handshake) {
        return registryKey(handshake.getDirection(), handshake.getSessionId());
    }
}
