/**
 *
 *
 * <pre>
 * <b>Description  : 기동 시 보안 필수 설정 검증(fail-fast)</b>
 * <b>Project Name : WooriCardCallBotRelayServer</b>
 * package  : com.woori.woorirelay.config
 * </pre>
 *
 * @author : RosieOh
 * @version : 1.0
 * @since
 *     <pre>
 * Modification Information
 *    수정일              수정자                수정내용
 * ---------------   ---------------   ----------------------------
 *  2026.07.07        RosieOh     보안 설정 fail-fast 추가
 *        </pre>
 */

package com.woori.woorirelay.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 운영 오설정이 조용히 서비스되는 것을 막는다. 인증/오리진 등 보안 필수값이 누락되면
 * 기동을 중단(prod)하거나 강한 경고를 남긴다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RelayConfigValidator implements ApplicationListener<ApplicationReadyEvent> {

    private final RelayProperties relayProperties;
    private final Environment environment;

    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        boolean prod = Arrays.asList(environment.getActiveProfiles()).contains("prod");

        RelayProperties.Auth wsAuth = relayProperties.getWebsocket().getAuth();
        if (wsAuth.isEnabled()) {
            if (wsAuth.getMode() == RelayProperties.Auth.Mode.HMAC && isBlank(wsAuth.getSecret())) {
                warn("WebSocket 인증(HMAC)이 활성인데 secret 이 비어 있습니다 — 모든 연결이 거절됩니다.", prod,
                        "prod 에서 RELAY_WS_AUTH_SECRET 는 필수입니다.");
            }
            if (wsAuth.getMode() == RelayProperties.Auth.Mode.STATIC && isBlank(wsAuth.getStaticTokens())) {
                warn("WebSocket 인증(STATIC)이 활성인데 static-tokens 가 비어 있습니다.", prod,
                        "prod 에서 static-tokens 는 필수입니다.");
            }
        } else {
            warn("WebSocket 인증이 비활성화되어 있습니다 — 운영에서는 반드시 활성화하세요.", prod,
                    "WebSocket 인증 비활성 상태로 prod 기동은 허용되지 않습니다.");
        }

        RelayProperties.Cti cti = relayProperties.getCti();
        if (cti.isEnabled() && cti.getAuth().isEnabled() && isBlank(cti.getAuth().getToken())) {
            warn("CTI 인증이 활성인데 token 이 비어 있습니다.", prod, "prod 에서 CTI_API_TOKEN 은 필수입니다.");
        }
        if (cti.isEnabled() && !cti.getAuth().isEnabled()) {
            warn("CTI 호출 인증이 비활성화되어 있습니다.", prod, "CTI 인증 없이 prod 기동은 허용되지 않습니다.");
        }

        String origins = relayProperties.getWebsocket().getAllowedOrigins();
        if ("*".equals(origins == null ? null : origins.trim())) {
            warn("websocket.allowed-origins 가 '*' 입니다 — CSWSH 위험.", prod,
                    "prod 에서 allowed-origins '*' 는 허용되지 않습니다. 도메인을 지정하세요.");
        }

        if (isBlank(environment.getProperty("spring.data.redis.password")) && prod) {
            warn("Redis 비밀번호가 비어 있습니다.", true, "prod 에서 REDIS_PASSWORD 는 필수입니다.");
        }

        log.info("[ConfigValidator] 보안 설정 검증 완료 (prod={})", prod);
    }

    private void warn(String message, boolean failInProd, String prodFailMessage) {
        if (failInProd) {
            fail(prodFailMessage);
        }
        log.warn("[ConfigValidator] {}", message);
    }

    private void fail(String message) {
        throw new IllegalStateException("[ConfigValidator] " + message);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
