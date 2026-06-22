# WooriCardCallBotRelayServer

우리카드 AI 콜봇 **실시간 음성 릴레이 미들웨어**.

고객/교환기 WebSocket(PCM) ↔ FastAPI AI Gateway 사이에서 오디오를 중계하고, STT/FDS 결과를 Redis·Kafka·CTI로 연결한다.

## 아키텍처

```
[고객/교환기] --Binary WS--> [WooriRelay:8080] --Binary WS--> [Callbot Gateway:8000]
                                    |    ^
                                    |    +-- Text/JSON (STT, FDS)
                                    +-- Redis (세션 상태)
                                    +-- Kafka (FDS 이벤트)
                                    +-- CTI REST (상담원 에스컬레이션)
```

### 핵심 클래스 흐름

| 클래스 | 역할 |
|--------|------|
| `VoiceIntermediaryHandler` | 클라이언트 WS 수립, PCM 수신 |
| `FastApiConnectionService` | 세션별 FastAPI 아웃바운드 WS 연결 |
| `FastApiBackendHandler` | AI Gateway JSON 결과 수신 |
| `VoicePipelineService` | 오디오 포워딩 + 결과 처리 오케스트레이션 |
| `VoiceSessionLifecycleService` | 에스컬레이션, 종료, 리소스 정리 |
| `RedisStateService` | 세션 상태 Hash (`wooricard:session:{id}`) |
| `KafkaProducerService` | `IntegrationContracts.TOPIC_FDS_EVENTS` 비동기 발행 |
| `AgentEscalationService` | CTI 에스컬레이션 요청 (Outbox 위임) |
| `CtiEscalationOutboxService` | CTI 실패 시 Redis Outbox 저장·재시도 |
| `DistributedSessionOwnershipService` | Redis 기반 세션 소유권 PoC |
| `PiiMaskingUtil` | 로그용 STT/카드/전화번호 마스킹 |

### 세션 라이프사이클

1. WebSocket 업그레이드 (경로로 방향 결정)
   - 인바운드: `/voice/inbound/{sessionId}`
   - 아웃바운드: `/voice/outbound/{sessionId}?campaignId=...`
   - 하위 호환: `/voice/{sessionId}` → INBOUND
2. Redis 세션 생성 + 방향별 FastAPI WS 연결
3. Binary PCM → FastAPI 포워딩
4. FastAPI JSON → Redis 갱신 + Kafka 발행 (`callDirection` 포함)
5. `AGENT_ESCALATION` 또는 `fds_flag=CRITICAL` → CTI 호출 + 세션 종료 (close code `4001`)
6. 종료 시 Kafka `SESSION_ENDED` 발행

## 로컬 실행

```bash
# 인프라 + 전체 스택 (docker-compose)
docker compose up -d

# Relay만 로컬 실행
./gradlew bootRun
```

환경 변수:

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `REDIS_HOST` | localhost | Redis 호스트 |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka 브로커 |
| `FASTAPI_WS_BASE_URL` | ws://localhost:8000/v1/stream | Gateway WS URL (fallback) |
| `FASTAPI_WS_INBOUND_BASE_URL` | ws://localhost:8000/v1/stream/inbound | 인바운드 Gateway WS |
| `FASTAPI_WS_OUTBOUND_BASE_URL` | ws://localhost:8000/v1/stream/outbound | 아웃바운드 Gateway WS |
| `CTI_ENABLED` | false | CTI 연동 활성화 |
| `CTI_BASE_URL` | http://localhost:9000 | CTI API URL |
| `CTI_OUTBOX_ENABLED` | true | CTI 실패 Outbox·재시도 |
| `DISTRIBUTED_SESSION_ENABLED` | false | 분산 세션 소유권 PoC |
| `RELAY_INSTANCE_ID` | (자동 생성) | Relay 인스턴스 식별자 |

## 운영 배포 주의사항

### 1. 스케일아웃 — Sticky Session 필수

`VoiceSessionRegistry`는 **JVM 로컬 메모리**이다. 동일 `sessionId`의 클라이언트 WS와 FastAPI 백엔드 WS가 **같은 Relay 인스턴스**에 있어야 한다.

- L4/L7 LB: `sessionId` 기반 sticky session 또는 consistent hash
- `sessionId` path(`/voice/{sessionId}`) 기반 라우팅 권장 — Client IP만으로는 부족한 경우가 많음
- 스케일 판단 지표: `relay.active_sessions` (Prometheus)

### 2. Graceful Shutdown

- `GracefulShutdownHandler`: 종료 시 활성 세션 drain + Kafka `SESSION_ENDED` 발행
- 배포·재시작 전 LB에서 해당 인스턴스 트래픽을 먼저 차단한 뒤 프로세스 종료
- 종료 대기 시간은 활성 통화 수에 맞게 충분히 확보 (기본 30초)

```yaml
# application.yml
spring.lifecycle.timeout-per-shutdown-phase: 30s
```

### 3. 헬스체크

| Endpoint | 용도 |
|----------|------|
| `/actuator/health` | Liveness |
| `/actuator/health` (Redis UP) | Readiness — Redis 연결 확인 |
| `/actuator/prometheus` | `relay.active_sessions` 등 메트릭 |

FastAPI Gateway 연결 가능 여부는 health에 포함되지 않음 → Gateway 장애 시 Relay는 세션 단위로 `FASTAPI_CONNECTION_FAILURE` 종료.

### 4. Kafka

- Partition key = `sessionId` → 동일 통화 이벤트 순서 보장
- Producer: `acks=all`, idempotence 활성화
- Relay 자체 DLQ 없음 → FdsGateway 쪽 DLQ(`IntegrationContracts.TOPIC_FDS_DLQ`)와 연계
- 토픽·이벤트 기본값은 `IntegrationContracts`가 SSOT

### 5. CTI 에스컬레이션 (Outbox)

- 즉시 CTI 호출 → 실패 시 Redis Outbox(`wooricard:cti:outbox:`)에 저장
- `CtiEscalationRetryScheduler`가 30초마다 재시도 (지수 백오프, 기본 최대 5회)
- 성공 시 `done:{sessionId}` 키로 멱등성 보장
- 최대 재시도 초과 시 `dead:{sessionId}`로 이동
- `CTI_ENABLED=false` 시 mock 응답 (개발용)
- Grafana: `relay_cti_outbox_pending`, `relay_cti_escalation_retries_total`

### 6. 분산 세션 소유권 PoC

- `DISTRIBUTED_SESSION_ENABLED=true` 시 Redis에 `wooricard:relay:owner:{sessionId}` 저장
- 다른 인스턴스가 살아 있으면 중복 접속 거부 (close `4002`)
- 인스턴스 heartbeat: `wooricard:relay:instance:{instanceId}`
- 20초마다 소유권 TTL 갱신 (`DistributedSessionHeartbeatScheduler`)
- 멀티 인스턴스 배포 전 PoC 검증용 — 기본값 `false`

### 7. 연동 계약 (`IntegrationContracts`)

- Kafka 토픽·이벤트 타입·`schemaVersion`의 **단일 기준**
- Python `integration_contract.py`와 값 동기화 유지
- `RelayConstants`, `StreamEventTypes`는 기존 import 호환용 위임 래퍼

### 8. 보안

- `woori.relay.websocket.allowed-origins`: 운영에서 반드시 제한
- `REDIS_PASSWORD` 설정
- STT 로그는 `PiiMaskingUtil`로 마스킹 — 운영 로그 레벨 `INFO` 유지 권장

## 아키텍처 개선 로드맵

### 단기 (현재 코드베이스)
- [x] WebSocket MDC `sessionId` 주입
- [x] PII 마스킹 강화 (카드/전화/연속 숫자)
- [x] `relay.active_sessions` 메트릭 + Grafana 패널
- [x] Graceful shutdown drain
- [x] CTI Outbox + 재시도
- [x] 분산 세션 소유권 PoC

### 중기
- FastAPI 연결 Circuit Breaker
- Outbox dead-letter 운영 알람 연동

### 장기
- sessionId 기반 LB 라우팅 정식화
- Kafka exactly-once + Schema Registry
- Multi-AZ DR (Redis Sentinel/Cluster, Kafka MirrorMaker)

## WebSocket 프로토콜

| 경로 | 방향 | 비고 |
|------|------|------|
| `/voice/inbound/{sessionId}` | INBOUND | 고객 인입 |
| `/voice/outbound/{sessionId}` | OUTBOUND | `?campaignId=` 쿼리 지원 |
| `/voice/{sessionId}` | INBOUND | 하위 호환 |

| 방향 | 프레임 | 내용 |
|------|--------|------|
| Client → Relay | Binary | PCM 오디오 청크 (max 64KB) |
| Relay → FastAPI | Binary | 동일 PCM |
| FastAPI → Relay | Text/JSON | STT, FDS 결과 |
| Relay → Client | Close 4001 | 상담원 에스컬레이션 |
| Relay → Client | Close 4002 | 중복 세션 |

## 모니터링

| 서비스 | URL | 용도 |
|--------|-----|------|
| Prometheus | `:9090` | 메트릭 scrape / 알람 rule |
| Alertmanager | `:9093` | 알람 라우팅 |
| Grafana | `:3000` (admin/admin) | 대시보드 `Woori Callbot Overview` |
| Jaeger | `:16686` | OpenTelemetry trace UI |

설정 파일: `./monitoring/prometheus.yml`, `./monitoring/prometheus/alerts.yml`, `./monitoring/alertmanager.yml`

### 주요 패널 / PromQL

- **Relay Active Sessions** — `relay_active_sessions{application="WooriRelay", direction!~"inbound|outbound"}`
- **Callbot Active Sessions** — `callbot_active_sessions{application="WooriCallbotGateway"}`
- **E2E 세션 비교** — Relay vs Callbot `direction` 라벨 (Grafana 변수 `$direction`)
- **CTI Outbox Pending** — `relay_cti_outbox_pending`
- **FDS DLQ** — `increase(fds_kafka_dlq_published_total[5m])`
- **Kafka Consumer Lag** — `fds_kafka_consumer_lag`

### 알람 (Prometheus rules)

- `ScrapeTargetDown`, `CtiOutboxPending`, `FdsDlqPublished`, `SessionCountDrift`, `KafkaConsumerLagHigh`, `GatewayConnectionFailures`

### 검증

```bash
# Java ↔ Python integration_contract 동기화
python scripts/sync_integration_contracts.py

# Python 단위 테스트
cd ../WooriCardCallBotGateway && pytest
cd ../WooriCardFDSGateway && pytest

# E2E smoke (compose up 후)
python scripts/e2e_smoke.py
```
