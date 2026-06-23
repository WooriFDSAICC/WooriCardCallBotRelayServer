/**
 *
 *
 * <pre>
 * <b>Description  : CTI 에스컬레이션 요청 DTO</b>
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

import com.woori.woorirelay.model.CallDirection;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CtiEscalationRequest {

    String registryKey;
    String sessionId;
    CallDirection callDirection;
    String campaignId;
    String customerId;
    String priority;
    String fdsFlag;
    String lastSttText;
    String reason;
    String eventType;
}
