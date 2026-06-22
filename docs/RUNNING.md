# WooriCardCallBotRelayServer ???�행 가?�드

Spring Boot 기반 **Voice Relay** ?�버?�니?? PBX/?�이?�러 WebSocket PCM??Callbot Gateway�?중계?�고, Kafka·Redis·CTI?� ?�동?�니??

## ?�전 ?�구?�항

| ??�� | 버전 |
|------|------|
| Docker Desktop | Compose v2 권장 |
| Java (로컬 ?�행 ?? | 21 |
| Gradle | wrapper ?�함 (`./gradlew`) |

?�제 ?�로?�트 경로 (compose 빌드 context 기�?):

```
MiddleWare/
?��??� WooriCardCallBotRelayServer/   ?????�로?�트
?��??� WooriCardCallBotGateway/
?��??� WooriCardFDSGateway/
?��??� WooriTritonMock/
?��??� WooriCtiMock/
```

---

## 1. ?�체 ?�택 ?�행 (권장)

Relay ?�렉?�리?�서 Docker Compose�?**??구간**????번에 ?�웁?�다.

```powershell
cd MiddleWare\WooriCardCallBotRelayServer
docker compose up --build
```

백그?�운???�행:

```powershell
docker compose up --build -d
```

종료:

```powershell
docker compose down
```

### ?�비???�트

| ?�비??| ?�트 | URL |
|--------|------|-----|
| Relay | 8080 | http://localhost:8080 |
| Callbot Gateway | 8000 | http://localhost:8000 |
| FDS Gateway | 8010 | http://localhost:8010 |
| Triton Mock | 8001 | http://localhost:8001 |
| CTI Mock | 9000 | http://localhost:9000 |
| Redis | 6379 | localhost:6379 |
| Kafka | 9092 | localhost:9092 |
| Prometheus | 9090 | http://localhost:9090 |
| Alertmanager | 9093 | http://localhost:9093 |
| Grafana | 3000 | http://localhost:3000 (admin/admin) |
| Jaeger | 16686 | http://localhost:16686 |

### Relay WebSocket ?�드?�인??(?�라?�언????Relay)

| 경로 | 방향 |
|------|------|
| `ws://localhost:8080/voice/inbound/{sessionId}` | INBOUND |
| `ws://localhost:8080/voice/outbound/{sessionId}?campaignId=CAMP01` | OUTBOUND |
| `ws://localhost:8080/voice/{sessionId}` | ?�거??(INBOUND) |

---

## 2. Relay ?�독 로컬 ?�행

Gateway·Kafka·Redis ??**?�존 ?�비?��? ?��? ???�어??* ?�니??

```powershell
cd MiddleWare\WooriCardCallBotRelayServer
.\gradlew bootRun
```

JAR 빌드 ???�행:

```powershell
.\gradlew bootJar -x test
java -jar build\libs\*.jar
```

### 주요 ?�경 변??

| 변??| 기본�?| ?�명 |
|------|--------|------|
| `REDIS_HOST` | localhost | Redis ?�스??|
| `REDIS_PORT` | 6379 | Redis ?�트 |
| `KAFKA_BOOTSTRAP_SERVERS` | localhost:9092 | Kafka |
| `KAFKA_FDS_TOPIC` | wooricard-fds-events | FDS ?�벤???�픽 |
| `FASTAPI_WS_INBOUND_BASE_URL` | ws://localhost:8000/v1/stream/inbound | Callbot INBOUND WS |
| `FASTAPI_WS_OUTBOUND_BASE_URL` | ws://localhost:8000/v1/stream/outbound | Callbot OUTBOUND WS |
| `CTI_ENABLED` | false | CTI ?�동 ?��? |
| `CTI_BASE_URL` | http://localhost:9000 | CTI Mock URL |
| `OTEL_EXPORTER_OTLP_ENDPOINT` | http://localhost:4318/v1/traces | Jaeger OTLP |

?�정 ?�일: `src/main/resources/application.yml`

---

## 3. ?�작 ?�인

### Health / Metrics

```powershell
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/prometheus
```

### E2E ?�모???�스??

?�체 ?�택 기동 ??

```powershell
cd MiddleWare\WooriCardCallBotRelayServer
pip install websockets
python scripts/e2e_smoke.py
```

?�션:

```powershell
python scripts/e2e_smoke.py --relay ws://localhost:8080 --cti http://localhost:9000
```

### Java ?�위 ?�스??

```powershell
.\gradlew test
```

### Integration contract ?�기??(Java ??Python)

```powershell
python ..\scripts\sync_integration_contracts.py
```

---

## 4. 모니?�링

- Grafana ?�?�보?? **Woori Callbot Overview** (`http://localhost:3000`)
- Prometheus ?�람 규칙: `monitoring/prometheus/alerts.yml`
- ?�정: `monitoring/prometheus.yml`, `monitoring/alertmanager.yml`

?�세??PromQL·?�람 목록?� ?�로?�트 루트 `README.md` 모니?�링 ?�션??참고?�세??

---

## 5. ?�러블슈??

| 증상 | ?�인 |
|------|------|
| Relay 기동 ?�패 | Callbot/FDS health ??`depends_on` ?�서 ?��?|
| Gateway ?�결 ?�패 | `FASTAPI_WS_*_BASE_URL`??compose ?�트?�크 ?�스?�명�??�치?�는지 |
| Kafka 발행 ?�패 | `woori-kafka` 컨테?�너 health, `KAFKA_BOOTSTRAP_SERVERS=kafka:9092` (compose ?��?) |
| CTI ?�스컬레?�션 ?�패 | `CTI_ENABLED=true`, `cti-mock:9000` ?�근 가???��? |
