# 기여 가이드 — 커밋 메시지

## 형식

```
<타입>: <한글 설명한다>. (#이슈번호)
```

### 타입

| 타입 | 용도 |
|------|------|
| `FEAT` | 기능 추가 |
| `FIX` | 버그 수정 |
| `CHORE` | 설정·빌드·인프라 |
| `TEST` | 테스트 |
| `DOCUMENT` | 문서 |

### 이슈 번호

- 커밋 메시지 **맨 끝**에 GitHub 이슈 번호를 붙입니다. 예: `(#10)`
- 한 커밋이 여러 이슈에 걸리면 **가장 핵심 이슈 하나**만 표기합니다.
- PR 본문에는 `Closes #1, #2` / `Ref #10` 형태로 추가 연결할 수 있습니다.

### 예시

```
FEAT: 인바운드·아웃바운드 Voice WebSocket 엔드포인트를 등록한다. (#1)
FIX: FastAPI 백엔드 연결 직후 disconnectCallback 버그를 수정한다. (#12)
CHORE: Prometheus scrape 대상에 Triton label을 확장한다. (#10)
```

### 금지

- `Co-authored-by: Cursor <cursoragent@cursor.com>` 등 에이전트 Co-authored-by 트레일러는 넣지 않습니다.

## Relay 이슈 매핑 (v1.1)

| 이슈 | 범위 |
|------|------|
| #1 | Voice WebSocket Relay·핸드셰이크·PCM 중계 |
| #2 | Redis 세션·Kafka FdsEvent |
| #3 | CTI 에스컬레이션·Outbox |
| #10 | Prometheus·Grafana·Alertmanager |
| #11 | E2E WebSocket 스모크 |
| #12 | FastAPI 백엔드 연결 버그 |
| #13 | 분산 세션 소유권·Graceful Shutdown |
| #14 | OpenTelemetry·Jaeger |

이슈 목록: https://github.com/WooriFDSAICC/WooriCardCallBotRelayServer/issues
