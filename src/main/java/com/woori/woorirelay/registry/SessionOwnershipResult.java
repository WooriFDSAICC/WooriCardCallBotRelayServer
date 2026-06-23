/**
 *
 *
 * <pre>
 * <b>Description  : 세션 소유권 claim 결과</b>
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

import lombok.Value;

@Value
public class SessionOwnershipResult {

    boolean claimed;
    String ownerInstanceId;

    public static SessionOwnershipResult claimed() {
        return new SessionOwnershipResult(true, null);
    }

    public static SessionOwnershipResult rejected(String ownerInstanceId) {
        return new SessionOwnershipResult(false, ownerInstanceId);
    }

    public boolean isRejected() {
        return !claimed;
    }
}
