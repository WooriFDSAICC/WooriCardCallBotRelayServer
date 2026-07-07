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
    private TtsWorker ttsWorker = new TtsWorker();
    private KafkaOutbox kafkaOutbox = new KafkaOutbox();
    private FastApi fastApi = new FastApi();

    public String resolveFastApiWsBaseUrl(CallDirection direction) {
        return switch (direction) {
            case OUTBOUND -> firstNonBlank(fastApiWsOutboundBaseUrl, fastApiWsBaseUrl);
            case INBOUND -> firstNonBlank(fastApiWsInboundBaseUrl, fastApiWsBaseUrl);
        };
    }

    public String resolveTtsWorkerWsBaseUrl(CallDirection direction) {
        return switch (direction) {
            case OUTBOUND -> firstNonBlank(ttsWorker.getOutboundBaseUrl(), ttsWorker.getBaseUrl());
            case INBOUND -> firstNonBlank(ttsWorker.getInboundBaseUrl(), ttsWorker.getBaseUrl());
        };
    }

    private static String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback;
    }

    /** 인스턴스당 동시 세션 상한(0 = 무제한). 초과 시 신규 연결 거절(close 4004). */
    private int maxSessionsPerInstance = 0;
    /** 세션별 업링크 오디오 큐 용량(청크 수). 초과 시 백프레셔로 드랍. */
    private int audioQueueCapacity = RelayConstants.DEFAULT_AUDIO_QUEUE_CAPACITY;

    @Getter
    @Setter
    public static class WebSocket {
        private String allowedOrigins = "*";
        private int maxTextMessageBufferSize = RelayConstants.DEFAULT_WS_TEXT_BUFFER_SIZE;
        private int maxBinaryMessageBufferSize = RelayConstants.DEFAULT_WS_BINARY_BUFFER_SIZE;
        private Auth auth = new Auth();
    }

    /**
     * WebSocket 핸드셰이크 인증. 교환기/게이트웨이가 발급한 단기 토큰을 검증한다.
     * <ul>
     *   <li>{@code HMAC} 모드: 토큰 = {@code <만료epochSec>.<hexHmacSha256(secret, sessionId+"."+만료)>}.
     *       sessionId 에 바인딩되어 재생·교차세션 재사용 불가.</li>
     *   <li>{@code STATIC} 모드: 사전 공유 토큰 집합(콤마 구분) 중 하나와 일치.</li>
     * </ul>
     */
    @Getter
    @Setter
    public static class Auth {
        private boolean enabled = true;
        private Mode mode = Mode.HMAC;
        /** HMAC 서명 비밀키(HMAC 모드 필수). RELAY_WS_AUTH_SECRET 환경변수로 주입. */
        private String secret = "";
        /** 사전 공유 토큰(STATIC 모드, 콤마 구분). */
        private String staticTokens = "";
        /** HMAC 토큰 허용 시계 오차(초). */
        private long clockSkewSeconds = 30;

        public enum Mode { HMAC, STATIC }
    }

    @Getter
    @Setter
    public static class TtsWorker {
        private boolean enabled = false;
        private String baseUrl = RelayConstants.DEFAULT_TTS_WORKER_WS_BASE_URL;
        private String inboundBaseUrl = RelayConstants.DEFAULT_TTS_WORKER_WS_INBOUND_BASE_URL;
        private String outboundBaseUrl = RelayConstants.DEFAULT_TTS_WORKER_WS_OUTBOUND_BASE_URL;
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
        private CtiAuth auth = new CtiAuth();
    }

    /**
     * CTI/ACD 호출 인증. 상담원 배정을 유발하는 내부 핵심계이므로 위조 호출 방지를 위해
     * 토큰/API키를 반드시 주입한다. 값은 비밀관리(Vault 등)에서 환경변수로 공급한다.
     */
    @Getter
    @Setter
    public static class CtiAuth {
        /** true 이고 CTI 활성 시 token 이 비어 있으면 기동 실패(fail-fast). */
        private boolean enabled = false;
        /** 인증 헤더명. 기본 Authorization(Bearer). API 키 방식이면 예: X-API-Key. */
        private String headerName = "Authorization";
        /** 헤더 값 스킴 프리픽스. Bearer 토큰이면 "Bearer ", API 키면 "" 로 둔다. */
        private String scheme = "Bearer ";
        /** 실제 토큰/키 값. CTI_API_TOKEN 등 환경변수로 주입. */
        private String token = "";

        public boolean isConfigured() {
            return enabled && token != null && !token.isBlank();
        }

        public String headerValue() {
            return (scheme == null ? "" : scheme) + token;
        }
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

    /**
     * FDS 이벤트 Kafka 발행 실패 시 Redis Outbox 영속화·재시도.
     * CTI Outbox 와 동일 패턴으로, 사기탐지 입력 이벤트의 유실을 방지한다.
     */
    @Getter
    @Setter
    public static class KafkaOutbox {
        private boolean enabled = true;
        private int maxAttempts = 8;
        private long initialRetryDelayMs = 10_000;
        private long retryIntervalMs = 10_000;
        private int batchSize = 50;
        private long sendTimeoutMs = 5_000;
        private String redisKeyPrefix = RelayConstants.DEFAULT_FDS_OUTBOX_KEY_PREFIX;

        public long retryDelayMs(int attempt) {
            return initialRetryDelayMs * (1L << Math.min(attempt, 6));
        }
    }

    /**
     * FastAPI Gateway 연결 복원력 — 재시도(백오프)와 Circuit Breaker.
     * 게이트웨이 순간 장애에 세션을 즉시 종료하지 않고 재시도하며, 지속 장애 시 CB 를 열어
     * thundering-herd 를 막고 빠르게 실패시킨다.
     */
    @Getter
    @Setter
    public static class FastApi {
        private int connectMaxAttempts = 2;
        private long connectBackoffMs = 200;
        private int cbFailureThreshold = 5;
        private long cbOpenMs = 10_000;
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
