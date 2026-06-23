# WooriCardCallBotRelayServer — 실행 가이드

Spring Boot 기반 **Voice Relay** 서버입니다. PBX/다이얼러 WebSocket PCM을 Callbot Gateway로 중계하고, Kafka·Redis·CTI와 연동합니다.

## 사전 요구사항

| 항목 | 버전 |
|------|------|
| Docker Desktop | Compose v2 권장 |
| Java (로컬 실행 시) | 21 |
| Gradle | wrapper 포함 (`./gradlew`) |

형제 프로젝트 경로 (compose 빌드 context 기준):

```
MiddleWare/
├── WooriCardCallBotRelayServer/   ← 이 프로젝트
├── WooriCardCallBotGateway/
├── WooriCardFDSGateway/
├── WooriTritonMock/
└── WooriCtiMock/
```

---

## 1. 전체 스택 실행 (권장)

Relay 디렉터리에서 Docker Compose로 **전 구간**을 한 번에 띄웁니다.

```powershell
cd MiddleWare\WooriCardCallBotRelayServer
docker compose up --build
```

백그라운드 실행:

```powershell
docker compose up --build -d
```

종료:

```powershell
docker compose down
```

### 서비스 포트

| 서비스 | 포트 | URL |
|--------|------|-----|
| Relay | 8080 | http://localhost:8080 |
| Callbot Gateway | 8000 | http://localhost:8000 |
| FDS Gateway | 8010 | http://localhost:8010 |
| Triton Mock | 8001 | http://localhost:8001 |
| CTI Mock | 9000 | http://localhost:9000 |
| Redis | 6380 | localhost:6380 (컨테이너 내부 6379) |
| Kafka | 9092 | localhost:9092 |
| Prometheus | 9091 | http://localhost:9091 |
| Alertmanager | 9093 | http://localhost:9093 |
| Grafana | 3000 | http://localhost:3000 (admin/admin) |
| Jaeger | 16686 | http://localhost:16686 |

### Relay WebSocket 엔드포인트 (클라이언트 → Relay)

| 경로 | 방향 |
|------|------|
| `ws://localhost:8080/voice/inbound/{sessionId}` | INBOUND |
| `ws://localhost:8080/voice/outbound/{sessionId}?campaignId=CAMP01` | OUTBOUND |
| `ws://localhost:8080/voice/{sessionId}` | 레거시 (INBOUND) |

---

## 2. Relay 단독 로컬 실행

Gateway·Kafka·Redis 등 **의존 서비스가 먼저 떠 있어야** 합니다.

```powershell
cd MiddleWare\WooriCardCallBotRelayServer
.\gradlew bootRun
```

JAR 빌드 후 실행:

```powershell
.\gradlew bootJar -x test
java -jar build\libs\*.jar
```

### 주요 환경 변수

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `REDIS_HOST` | localhost | Redis 호스트 |
| `REDIS_PORT` | 6379 | Redis 포트 (로컬 단독 시 compose는 6380) |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `KAFKA_FDS_TOPIC` | wooricard-fds-events | FDS 이벤트 토픽 |
| `FASTAPI_WS_INBOUND_BASE_URL` | ws://localhost:8000/v1/stream/inbound | Callbot INBOUND WS |
| `FASTAPI_WS_OUTBOUND_BASE_URL` | ws://localhost:8000/v1/stream/outbound | Callbot OUTBOUND WS |
| `CTI_ENABLED` | false | CTI 연동 여부 |
| `CTI_BASE_URL` | http://localhost:9000 | CTI Mock URL |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | http://localhost:4318/v1/traces | Jaeger OTLP |

설정 파일: `src/main/resources/application.yml`

---

## 3. 동작 확인

### Health / Metrics

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

### E2E 스모크 테스트

전체 스택 기동 후:

```powershell
cd MiddleWare\WooriCardCallBotRelayServer
pip install websockets
python scripts/e2e_smoke.py
```

옵션:

```powershell
python scripts/e2e_smoke.py --relay ws://localhost:8080 --cti http://localhost:9000
```

### Java 단위 테스트

```powershell
.\gradlew test
```

### Integration contract 동기화 (Java ↔ Python)

```powershell
python ..\scripts\sync_integration_contracts.py
```

---

## 4. 모니터링

- Grafana 대시보드: **Woori Callbot Overview** (`http://localhost:3000`)
- Prometheus 알람 규칙: `monitoring/prometheus/alerts.yml`
- 설정: `monitoring/prometheus.yml`, `monitoring/alertmanager.yml`

자세한 PromQL·알람 목록은 프로젝트 루트 `README.md` 모니터링 섹션을 참고하세요.

---

## 5. 트러블슈팅

| 증상 | 확인 |
|------|------|
| Relay 기동 실패 | Callbot/FDS health 및 `depends_on` 순서 확인 |
| Gateway 연결 실패 | `FASTAPI_WS_*_BASE_URL`이 compose 네트워크 호스트명과 일치하는지 |
| Kafka 발행 실패 | `woori-kafka` 컨테이너 health, `KAFKA_BOOTSTRAP_SERVERS=kafka:9092` (compose 내부) |
| CTI 에스컬레이션 실패 | `CTI_ENABLED=true`, `cti-mock:9000` 접근 가능 여부 |
