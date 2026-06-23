/**
 *
 *
 * <pre>
 * <b>Description  : WebSocket 연결 handshake 모델</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.model
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

package com.woori.woorirelay.model;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VoiceSessionHandshake {

    String sessionId;
    CallDirection direction;
    String campaignId;

    public String getRegistryKey() {
        return com.woori.woorirelay.support.SessionRegistryKeys.registryKey(direction, sessionId);
    }
}
