/**
 *
 *
 * <pre>
 * <b>Description  : CTI 에스컬레이션 응답 DTO</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.integration.cti
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

package com.woori.woorirelay.integration.cti;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CtiEscalationResponse {

    boolean accepted;
    String queueId;
    String message;
}
