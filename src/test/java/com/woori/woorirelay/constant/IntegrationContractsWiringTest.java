/**
 *
 *
 * <pre>
 * <b>Description  : IntegrationContractsWiringTest 단위 테스트</b>
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IntegrationContractsWiringTest {

    @Test
    void relayConstants_kafkaTopicDelegatesToIntegrationContracts() {
        assertEquals(IntegrationContracts.TOPIC_FDS_EVENTS, RelayConstants.DEFAULT_KAFKA_TOPIC);
    }

    @Test
    void streamEventTypes_delegateToIntegrationContracts() {
        assertEquals(IntegrationContracts.EVENT_STT_PARTIAL, StreamEventTypes.STT_PARTIAL);
        assertEquals(IntegrationContracts.EVENT_AGENT_ESCALATION, StreamEventTypes.AGENT_ESCALATION);
        assertEquals(IntegrationContracts.EVENT_SESSION_ENDED, StreamEventTypes.SESSION_ENDED);
    }

    @Test
    void callDirectionConstants_matchPythonContract() {
        assertEquals("INBOUND", IntegrationContracts.CALL_DIRECTION_INBOUND);
        assertEquals("OUTBOUND", IntegrationContracts.CALL_DIRECTION_OUTBOUND);
        assertEquals("wooricard:session:", IntegrationContracts.SESSION_REDIS_KEY_PREFIX);
    }
}
