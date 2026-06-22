/**
 *
 *
 * <pre>
 * <b>Description  : Relay 설정 프로퍼티 바인딩</b>
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
 *  2026.06.22        RosieOh     최초생성
 *        </pre>
 */

package com.woori.woorirelay.config;

import com.woori.woorirelay.constant.CtiConstants;
import com.woori.woorirelay.constant.IntegrationContracts;
import com.woori.woorirelay.constant.RelayConstants;
import com.woori.woorirelay.model.CallDirection;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "woori.relay")
public class RelayProperties {

    private String fastApiWsBaseUrl = RelayConstants.DEFAULT_FASTAPI_WS_BASE_URL;
    private String fastApiWsInboundBaseUrl = RelayConstants.DEFAULT_FASTAPI_WS_INBOUND_BASE_URL;
    private String fastApiWsOutboundBaseUrl = RelayConstants.DEFAULT_FASTAPI_WS_OUTBOUND_BASE_URL;
    private String kafkaTopic = IntegrationContracts.TOPIC_FDS_EVENTS;
    private String redisSessionKeyPrefix = RelayConstants.DEFAULT_REDIS_SESSION_KEY_PREFIX;
    private int maxBinaryChunkBytes = RelayConstants.DEFAULT_MAX_BINARY_CHUNK_BYTES;

    private WebSocket websocket = new WebSocket();
    private Cti cti = new Cti();
    private DistributedSession distributedSession = new DistributedSession();

    public String resolveFastApiWsBaseUrl(CallDirection direction) {
        return switch (direction) {
            case OUTBOUND -> firstNonBlank(fastApiWsOutboundBaseUrl, fastApiWsBaseUrl);
            case INBOUND -> firstNonBlank(fastApiWsInboundBaseUrl, fastApiWsBaseUrl);
        };
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    @Getter
    @Setter
    public static class WebSocket {
        private String allowedOrigins = "*";
        private int maxTextMessageBufferSize = RelayConstants.DEFAULT_WS_TEXT_BUFFER_SIZE;
        private int maxBinaryMessageBufferSize = RelayConstants.DEFAULT_WS_BINARY_BUFFER_SIZE;
    }

    @Getter
    @Setter
    public static class Cti {
        private boolean enabled = false;
        private String baseUrl = "http://localhost:9000";
        private String escalationPath = CtiConstants.DEFAULT_ESCALATION_PATH;
        private int connectTimeoutMs = 3_000;
        private int readTimeoutMs = 5_000;
        private CtiOutbox outbox = new CtiOutbox();
    }

    @Getter
    @Setter
    public static class CtiOutbox {
        private boolean enabled = true;
        private int maxAttempts = 5;
        private long initialRetryDelayMs = 30_000;
        private long retryIntervalMs = 30_000;
        private int batchSize = 10;
        private String redisKeyPrefix = "wooricard:cti:outbox:";

        public long retryDelayMs(int attempt) {
            return initialRetryDelayMs * (1L << Math.min(attempt, 6));
        }
    }

    @Getter
    @Setter
    public static class DistributedSession {
        private boolean enabled = false;
        private String instanceId = "";
        private long ownerTtlSeconds = 60;
        private long heartbeatIntervalMs = 20_000;
        private String redisOwnerKeyPrefix = "wooricard:relay:owner:";
        private String redisInstanceKeyPrefix = "wooricard:relay:";
    }
}
