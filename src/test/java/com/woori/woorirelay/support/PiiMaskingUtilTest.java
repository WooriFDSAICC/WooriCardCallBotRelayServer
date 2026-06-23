/**
 *
 *
 * <pre>
 * <b>Description  : PiiMaskingUtilTest 단위 테스트</b>
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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiiMaskingUtilTest {

    @Test
    void maskSttText_masksPhoneNumber() {
        String result = PiiMaskingUtil.maskSttText("제 번호는 010-1234-5678 입니다");
        assertTrue(result.startsWith("제 번"));
        assertTrue(!result.contains("010-1234-5678"));
        assertTrue(!result.contains("5678"));
    }

    @Test
    void maskSttText_masksCardNumber() {
        String result = PiiMaskingUtil.maskSttText("카드번호 1234-5678-9012-3456");
        assertTrue(result.startsWith("카드번"));
        assertTrue(!result.contains("1234-5678-9012-3456"));
        assertTrue(!result.contains("3456"));
    }

    @Test
    void maskForLog_truncatesLongPayload() {
        String longText = "가".repeat(200);
        String result = PiiMaskingUtil.maskForLog(longText);
        assertTrue(result.endsWith("...(truncated)"));
        assertTrue(result.length() < longText.length());
    }

    @Test
    void maskUserId_shortId() {
        assertEquals("****", PiiMaskingUtil.maskUserId("abcd"));
    }

    @Test
    void maskUserId_longId() {
        assertEquals("us****45", PiiMaskingUtil.maskUserId("user12345"));
    }
}
