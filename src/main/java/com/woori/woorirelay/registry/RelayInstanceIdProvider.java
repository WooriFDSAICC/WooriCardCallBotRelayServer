/**
 *
 *
 * <pre>
 * <b>Description  : Relay 인스턴스 식별자 제공</b>
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

import com.woori.woorirelay.config.RelayProperties;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.UUID;

/**
 * Relay 인스턴스 식별자 — 분산 세션 소유권 PoC용.
 */
@Component
public class RelayInstanceIdProvider {

    private final RelayProperties relayProperties;

    @Getter
    private String instanceId;

    public RelayInstanceIdProvider(RelayProperties relayProperties) {
        this.relayProperties = relayProperties;
    }

    @PostConstruct
    void init() {
        String configured = relayProperties.getDistributedSession().getInstanceId();
        if (configured != null && !configured.isBlank()) {
            instanceId = configured.trim();
            return;
        }
        instanceId = resolveHostName() + "-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private static String resolveHostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException ex) {
            return "relay";
        }
    }
}
