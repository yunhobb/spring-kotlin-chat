# 실시간 · 세션 계층 설계

> 다음 구현 대상(`ws-gateway`, `fanout`)의 상세 설계. 상위 맥락은 [architecture.md](architecture.md) §3, 계약은 [legacy-api.md](legacy-api.md).
> 상태: **설계 확정 + 일부 미결(하단 §7)**. 구현은 이 문서를 기준으로 TDD한다.

## 1. 두 개의 세션 장부 (핵심 개념)

세션 정보를 **두 곳**에 나눠 둔다. 역할이 다르다.

| 장부 | 위치 | 담는 것 | 용도 |
|---|---|---|---|
| `LocalSessionMap` | 파드 인메모리 | storeSeq → 이 파드의 WebSocketSession 집합 | **실제 push 대상 소켓** 조회 |
| `SessionRegistry` | Redis(ElastiCache) | storeSeq → 붙어있는 파드 목록(+deviceId) | **타겟 라우팅**(어느 파드에 발행?) + **오프라인 판정** |

원칙: 로컬 맵은 "이 파드가 가진 소켓", 레지스트리는 "전 클러스터에서 이 유저가 어디 있나". 둘은 연결 수립/종료 때 함께 갱신된다.

## 2. 연결 수명주기

```
클라 ──WS 핸드셰이크(JWT)──▶ 파드
  1) 핸드셰이크 인터셉터: JWT 검증 → storeSeq 추출 → 세션 attributes에 저장
  2) afterConnectionEstablished:
       LocalSessionMap.add(storeSeq, session)
       SessionRegistry.register(storeSeq, {podId, deviceId})   // TTL 세팅
  3) 하트비트마다: SessionRegistry TTL 갱신(heartbeat → refresh)
  4) afterConnectionClosed:
       LocalSessionMap.remove(storeSeq, session)
       SessionRegistry.unregister(storeSeq, {podId, deviceId})
```

- **인증은 핸드셰이크에서 끝낸다.** storeSeq를 세션 attribute에 넣어두면 이후 프레임마다 재검증 불필요. (CLAUDE.md: JWT Bearer, REST/WS 동일)
- **파드 강제 종료(OOM/kill) 대비**: unregister가 실행 안 될 수 있으므로 레지스트리 키에 **TTL**을 걸고 하트비트로 갱신한다. 최종 안전망은 TTL 만료(유령 엔트리 자동 소멸).

## 3. LocalSessionMap 동시성 모델 (결정)

여러 스레드가 동시에 add/remove 한다(가상 스레드 위 요청들). 현재 순진한 구현에는 **잃어버린 갱신(lost-update) 레이스**가 있다:

```
remove: set.isEmpty() 확인 → (그 사이 다른 스레드가 add) → map.remove(key, set) 로
        비어있지 않게 된 set을 통째로 제거 → 방금 add된 세션이 맵에서 유실
```

**결정: add/remove의 집합 변경과 정리(cleanup)를 `ConcurrentHashMap.compute`의 키 단위 원자 블록 안에서 수행한다.**

```kotlin
fun add(storeSeq, session) = sessions.compute(storeSeq) { _, set ->
    (set ?: newKeySet()).also { it.add(session) }
}
fun remove(storeSeq, session) = sessions.computeIfPresent(storeSeq) { _, set ->
    set.remove(session); if (set.isEmpty()) null else set   // null 반환 = 엔트리 원자적 제거
}
```

`sessionsOf`는 **불변 스냅샷**을 반환한다(내부 가변 집합 노출 금지, 브로드캐스트 중 외부 변경 차단). 조회용 `isOnline(storeSeq)`도 제공한다.

## 4. 메시지 팬아웃 흐름

```
전송 유스케이스(SendMessageService):
  1. MessageRepository.append        (DynamoDB, source of truth — 먼저)
  2. 수신자별 SessionRegistry.locate
  3. 온라인(파드 존재) → FanoutPublisher.publish(파드채널, message)
     오프라인(빈 목록)  → OfflinePushQueue.enqueue (SQS)
```

- **파드 채널**: `chat:pod:{podId}`. 각 파드는 자기 채널만 구독(`PodChannelSubscriber`)하고, 수신 메시지를 `LocalSessionMap`에서 찾아 push.
- **의존 방향 유지**: 수신측 "Redis 구독 → 로컬 소켓 push"는 `chat-core`에 inbound port(예: `MessagePushTarget`)를 두고 `fanout`이 그것을 호출한다. `fanout`이 `ws-gateway`를 직접 참조하면 게이트웨이 분리가 막힌다(CLAUDE.md 의존 규칙).
- **정합성**: Redis Pub/Sub은 best-effort. 유실 시 클라 재조회(REST)·재연결이 DynamoDB에서 복구. 실시간 홉을 신뢰성 채널로 취급하지 않는다.

## 5. SessionRegistry 키 구조 (Redis)

```
key:  chat:sess:{storeSeq}           (Redis Hash 또는 Set)
val:  {podId} → {deviceId, lastSeenAt}
TTL:  하트비트 주기 × 3 (미확정 — §7)
```

- `register`: 필드 추가 + TTL 갱신
- `unregister`: 필드 제거(비면 키 삭제)
- `locate`: 필드 전체 조회 → `List<SessionLocation>`; 비어 있으면 오프라인
- 멀티 디바이스: 한 storeSeq에 여러 (podId, deviceId) 공존 가능

## 6. 팬아웃 페이로드 포맷 (우리 소관 — 레거시 계약 아님)

파드 간 내부 전송 포맷이므로 **우리가 자유롭게 정한다**. 클라이언트로 나가는 WS 프레임(§7, 레거시)과는 별개.

```jsonc
{ "target": { "storeSeq": 200, "deviceId": null },
  "message": { /* 도메인 Message 직렬화 */ } }
```

- 컷오버 Phase 2의 **신구 팬아웃 브리지**와 포맷을 맞춰야 함([architecture.md](architecture.md) §5, §6-3 조사 후 확정).
- 직렬화: `chat-core` 도메인은 프레임워크 무의존이므로, 직렬화는 `fanout` 어댑터에서 수행(도메인에 Jackson 누수 금지).

## 7. 미결 / 조사 대기

| 항목 | 상태 | 막는 것 |
|---|---|---|
| **클라이언트 WS 프레임 프로토콜** | 미확보 | 웹엔 실시간 채널 없음 → 앱/구서버 소스 필요([legacy-api.md](legacy-api.md), architecture §6-1). `TALK`/`ack`/`sync` 키워드만 확인 |
| **하트비트 주기 ↔ ALB idle timeout** | 미확정 | 레지스트리 TTL·close 드레인 속도가 여기 종속(§6-4) |
| **발신자 멀티디바이스 에코** | 미결정 | 현재 `SendMessageService`는 발신자를 수신 대상에서 제외 → 폰에서 보낸 메시지가 같은 사용자의 데스크톱엔 실시간 전달 안 됨. 레거시가 로컬 에코인지 재조회인지(§6-1) 확인 후 결정. [ADR-0003](adr/0003-pure-websocket-protocol.md) 참조 |
| 구 시스템 팬아웃 내부 구조 | 미확보 | Phase 2 브리지 설계(§6-3) |

## 8. 배포 드레인 (EKS)

- preStop: 신규 연결 차단 → 기존 연결에 close frame을 **수 분에 걸쳐 분산 발송**(재연결 폭풍 방지) → 종료.
- 클라 수정 불가(계약 유지)이므로 재연결 폭풍은 **서버 드레인 속도**로만 제어한다.
- `PodDisruptionBudget` 필수. 상세는 [architecture.md](architecture.md) §7.
