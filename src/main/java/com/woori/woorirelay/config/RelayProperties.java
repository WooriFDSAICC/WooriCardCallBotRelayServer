package com.woori.woorirelay.config;

import com.woori.woorirelay.constant.RelayConstants;
import com.woori.woorirelay.constant.WebSocketConstants;
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
    private String kafkaTopic = RelayConstants.DEFAULT_KAFKA_TOPIC;
    private String redisSessionKeyPrefix = RelayConstants.DEFAULT_REDIS_SESSION_KEY_PREFIX;
    private int maxBinaryChunkBytes = RelayConstants.DEFAULT_MAX_BINARY_CHUNK_BYTES;

    private WebSocket websocket = new WebSocket();
    private Cti cti = new Cti();

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
        private String escalationPath = com.woori.woorirelay.constant.CtiConstants.DEFAULT_ESCALATION_PATH;
        private int connectTimeoutMs = 3_000;
        private int readTimeoutMs = 5_000;
    }
}
