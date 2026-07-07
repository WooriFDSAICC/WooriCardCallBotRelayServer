/**
 *
 *
 * <pre>
 * <b>Description  : PiiMaskingUtil.redactPii 단위 테스트(H2)</b>
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
 *  2026.07.07        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PiiMaskingUtilRedactTest {

    @Test
    void redactPii_masksCardNumber_butKeepsSurroundingWords() {
        String result = PiiMaskingUtil.redactPii("카드번호 1234-5678-9012-3456 결제해줘");
        assertTrue(result.startsWith("카드번호 "));
        assertTrue(result.endsWith(" 결제해줘"));
        assertFalse(result.contains("1234-5678-9012-3456"));
        assertFalse(result.contains("3456"));
    }

    @Test
    void redactPii_masksResidentRegistrationNumber() {
        String result = PiiMaskingUtil.redactPii("주민번호 990101-1234567 확인");
        assertFalse(result.contains("990101-1234567"));
        assertFalse(result.contains("1234567"));
        assertTrue(result.contains("확인"));
    }

    @Test
    void redactPii_masksPhone() {
        String result = PiiMaskingUtil.redactPii("연락처 010-1234-5678");
        assertFalse(result.contains("010-1234-5678"));
        assertTrue(result.startsWith("연락처"));
    }

    @Test
    void redactPii_preservesNonPiiText() {
        String result = PiiMaskingUtil.redactPii("계좌 이체가 안돼요 도와주세요");
        assertEquals("계좌 이체가 안돼요 도와주세요", result);
    }

    @Test
    void redactPii_nullAndBlankPassThrough() {
        assertEquals(null, PiiMaskingUtil.redactPii(null));
        assertEquals("", PiiMaskingUtil.redactPii(""));
    }
}
