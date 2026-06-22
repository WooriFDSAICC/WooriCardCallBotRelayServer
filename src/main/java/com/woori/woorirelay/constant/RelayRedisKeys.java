/**
 *
 *
 * <pre>
 * <b>Description  : Redis 키 접미사 상수</b>
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

@UtilityClass
public final class RelayRedisKeys {

    public static final String CTI_OUTBOX_QUEUE_SUFFIX = "queue";
    public static final String CTI_OUTBOX_ITEM_SUFFIX = "item:";
    public static final String CTI_ESCALATION_DONE_SUFFIX = "done:";
    public static final String CTI_OUTBOX_DEAD_SUFFIX = "dead:";

    public static final String RELAY_OWNER_SUFFIX = "owner:";
    public static final String RELAY_INSTANCE_SUFFIX = "instance:";
}
