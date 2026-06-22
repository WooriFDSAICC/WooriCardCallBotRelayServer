/**
 *
 *
 * <pre>
 * <b>Description  : 스트림 이벤트 타입 상수 (IntegrationContracts 위임)</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.constant
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

package com.woori.woorirelay.constant;

import lombok.experimental.UtilityClass;

/**
 * Relay 내부 이벤트 타입 — {@link IntegrationContracts} 위임.
 * 기존 import 호환용 래퍼이며, 신규 코드는 IntegrationContracts를 직접 참조해도 된다.
 */
@UtilityClass
public final class StreamEventTypes {

    public static final String STT_PARTIAL = IntegrationContracts.EVENT_STT_PARTIAL;
    public static final String AGENT_ESCALATION = IntegrationContracts.EVENT_AGENT_ESCALATION;
    public static final String SESSION_ENDED = IntegrationContracts.EVENT_SESSION_ENDED;
}
