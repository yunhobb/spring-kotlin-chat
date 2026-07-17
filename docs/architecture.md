# 마켓플레이스 채팅 서버 재구축 설계

> 2026-07-17 설계 인터뷰 확정본.
> 성격: 데이터 마이그레이션이 아닌 **서버 교체(strangler-fig)** — 기존 DynamoDB 데이터·API 계약·WS 프로토콜을 유지한 채 서버 구현만 Kotlin Spring으로 재구축.

## 1. 확정된 결정 사항

| # | 항목 | 결정 | 근거 |
|---|---|---|---|
| 1 | 제품 형태 | 1:1 상품 문맥 채팅 (그룹 없음) | 마켓플레이스 실사 결과. roomId = `{storeSeq}_{storeSeq}` 유저쌍 기반, 상품 카드는 방의 컨텍스트 |
| 2 | 규모 | MAU 110만 → 300만 목표, 피크 동접 5만~10만 (상시 연결) | DAU 25%, 동접률 ~10% 추정 |
| 3 | 서버 스택 | Kotlin + Spring Boot **MVC** + JDK 21 가상 스레드 | 이 규모에서 WebFlux 이득은 커넥션 레이어뿐. 가상 스레드로 블로킹 IO 논거 해소. Tomcat WS는 NIO라 유휴 연결이 스레드 비점유 |
| 4 | WebFlux 전환 경로 | `ws-gateway` 모듈을 port 경계로 격리 → 필요 시 Netty/WebFlux 별도 배포체로 추출 | 한 앱에서 MVC+WebFlux 서버 동시 불가. 모듈 경계 = 미래의 네트워크 경계 |
| 5 | WS 프로토콜 | **기존 앱 프레임 프로토콜 그대로 유지** (앱 무수정) | 계약 유지 원칙. seq/ack/resync 신규 프로토콜은 v2로 보류 |
| 6 | REST 계약 | 기존 `/api/chat/*` 전체 유지 (하단 인벤토리 참조) | 앱 강제업데이트 없이 서버만 교체 |
| 7 | 저장소 | **기존 DynamoDB 테이블 유지** (신규 스키마 설계 아님) | "저장소는 유지" 확정. AWS SDK v2 Enhanced Client(동기) 사용, JPA 불사용 |
| 8 | 파드 간 팬아웃 | **Redis Pub/Sub + 세션 레지스트리** (ElastiCache 신규 도입 — 유일한 신규 인프라) | SQS는 1消費者 모델이라 팬아웃 부적합. 실시간 홉은 best-effort, 정합성은 DB가 보장 |
| 9 | 비동기 파이프라인 | 기존 **SQS** 유지 — 오프라인 푸시(FCM/APNs), 사기 감지 연동, 배지 집계 | 유실 불가 작업은 큐로 |
| 10 | 인증 | JWT Bearer (REST + WS 핸드셰이크 동일) | 기존 체계 |
| 11 | 아키텍처 | 모놀리스 우선, Gradle 멀티모듈로 관심사 격리 | "진짜 필요할 때만 분리" 원칙 |

## 2. 기존 시스템 실사 결과 (2026-07-17, web.example.com)

- 채팅 백엔드: `edge-live.example.com` (자체 구축 확인 — Sendbird 등 SaaS 흔적 없음)
- 미디어 CDN: `chat-media.example.com`
- 웹 클라이언트는 REST + 안읽음 폴링 위주, 번들에 `wss://socket...` 존재 (앱/조건부 사용 추정)
- 번들 내 프로토콜 힌트: `TALK`, `ack`, `sync` 키워드 확인

### REST API 인벤토리 (유지 대상 계약)
```
GET  /api/chat/rooms
GET  /api/chat/rooms/{roomId}                  # roomId = {storeSeq}_{storeSeq}
GET  /api/chat/rooms/{roomId}/messages
GET  /api/chat/users/{storeSeq}/unread-message-cnt
GET  /api/chat/users/{storeSeq}/region
POST /api/chat/users/{storeSeq}/block-user/{blockStoreSeq}
GET  /api/chat/products/{productSeq}/chat-count
GET  /api/chat/emoticons
GET  /api/chat/quick-buttons
# + cross-function-api.example.com/user/info/chat-available (타 서비스 소관)
```

> 레거시 REST 계약 상세 관측 결과는 **[legacy-api.md](legacy-api.md)** 참조 (2026-07-17 완료, §6-1 부분).
> 핵심 발견: ① 웹엔 실시간 채널 없음(WS는 앱 전용) ② 읽음은 per-message 플래그가 아니라 **멤버별 `lastReadAt`(epoch millis) 워터마크** ③ messageType = textMessage/productMessage/noticeMessage ④ messageId는 number 시퀀스 ⑤ 응답 봉투 `{meta, data}`.

### 기능 패리티 목록
- 읽음 표시(멤버별 lastReadAt 워터마크 비교), 전역 안읽음 배지
- 상품 카드 + 안심결제 "구매하기" 버튼 (커머스 결합)
- 이미지 전송, 이모티콘, 퀵버튼
- 사기 감지 시스템 메시지 ("연락처 거래 유도 채팅 감지") — 메시지 분석 파이프라인 훅
- 유저 차단, 탈퇴 회원 채팅 불가, 직거래 지역 표시
- 온라인 상태·타이핑 인디케이터 **없음** (v1 범위 제외)

## 3. 목표 아키텍처

```
클라(앱/웹) ══WS+REST══ ALB ── [EKS: chat 파드 × 10~20]
                                │  (파드당 WS 5천~1만)
        ┌───────────────────────┼──────────────────────┐
        ▼                       ▼                      ▼
  ElastiCache Redis        DynamoDB(기존 테이블)      SQS(기존)
  - Pub/Sub(파드 간 팬아웃)  - 메시지/방/멤버/읽음      - 푸시 알림 워커
  - 세션 레지스트리          - source of truth        - 사기 감지 연동
    user→pod 매핑, TTL                                - 배지/집계
```

메시지 흐름 (전송):
1. 발신 파드: DynamoDB 저장 (source of truth)
2. 세션 레지스트리 조회 → 수신자가 붙은 파드의 채널(`pod:{podId}`)에 Redis 발행
3. 수신 파드: 로컬 세션맵에서 소켓 찾아 push
4. 수신자 오프라인(레지스트리 miss) → SQS로 푸시 알림 작업 발행
5. Redis 유실 시에도 정합성은 클라 재조회/재연결 경로가 DB에서 복구

## 4. 모듈 구조 (Gradle 멀티모듈)

```
app/              # Spring Boot 조립·설정 (web-application-type: servlet)
chat-core/        # 도메인+유스케이스. 프레임워크·AWS SDK 무의존. port 인터페이스 소유
chat-api/         # REST 컨트롤러 — 기존 /api/chat/* 계약 구현
ws-gateway/       # WS 핸드셔이크·프레임 코덱·로컬 세션맵. chat-core를 port로만 호출
fanout/           # Redis Pub/Sub 어댑터 + 세션 레지스트리
storage-dynamo/   # DynamoDB 어댑터 (기존 스키마 매핑)
async-sqs/        # SQS 프로듀서/컨슈머 (푸시·사기감지·집계)
integration/      # 상품/유저/결제 내부 API 클라이언트 (anti-corruption layer)
```

의존 규칙(헥사고날): `chat-api`, `ws-gateway` → `chat-core` ← `storage-dynamo`, `fanout`, `async-sqs`, `integration`

**분리 우선순위 (관심사가 달라지는 선):**
1. `ws-gateway` → Netty/WebFlux 경량 게이트웨이 (연결 밀도·배포 주기가 다른 유일한 레이어)
2. `async-sqs` 워커 → 별도 디플로이먼트 (스케일 특성이 다름)
3. 나머지는 분리 근거가 생기기 전까지 모놀리스 유지

## 5. 컷오버 전략

같은 DB를 신/구 서버가 동시에 보므로 데이터 이관은 없음. 단계:

- **Phase 0 — 선행 조사** (아래 6장). 섀도 리드: GET 트래픽 미러링, 신/구 응답 diff
- **Phase 1 — 읽기 카나리**: rooms/messages/unread-cnt를 ALB 가중치 1%→100%
- **Phase 2 — 쓰기+WS 카나리**: 신구 **팬아웃 브리지** 가동(신 서버가 구 시스템의 알림 채널에도 발행, 역방향 동일) — 신구에 걸친 두 유저 간 실시간 보장
- **Phase 3 — 100% 전환**: 구 서버·브리지 제거
- **Phase 4 (선택, v2)**: seq/ack/resync 프로토콜, ws-gateway Netty 분리, 스키마 개선

## 6. 선행 조사 항목 (구현 착수 전 필수)

1. **기존 WS 프레임 규격 역설계** — 구 소켓 서버 코드/앱 코드에서 접속·인증·메시지·읽음·하트비트 프레임 규격 확보 (`TALK`/`ack`/`sync` 확인됨)
2. **DynamoDB 테이블 스키마 문서화** — PK/SK 구조, GSI, 용량 모드, 읽음 상태 저장 방식, 구 서버만 아는 숨은 필드
3. **구 시스템 팬아웃 내부 구조** — Phase 2 브리지 설계에 필수. 구 서버가 파드 간 전달에 뭘 쓰는지(Redis? 인메모리 단일 노드? SQS?)
4. 하트비트 주기 ↔ ALB idle timeout 정합 확인
5. 사기 감지 파이프라인 인터페이스 (동기 훅인지 비동기 큐인지)

## 7. EKS 운영 설계

- **HPA**: WS 연결 수 커스텀 메트릭(우선) + CPU 보조. 파드당 목표 연결 상한 8천
- **배포 드레인**: preStop에서 신규 연결 차단 → 기존 연결에 close frame을 수 분에 걸쳐 분산 발송(재연결 폭풍 방지) → 종료. PodDisruptionBudget 필수
- **재연결**: 클라 수정 불가(계약 유지)이므로 서버 측 드레인 속도로 폭풍 제어
- **관측성**: 연결 수/파드, 메시지 저장→전달 e2e p99, Redis 발행→수신 지연, DynamoDB 스로틀, WS 비정상 종료율
- **Graceful 재시작 리허설**: 전체 롤링 시 5만 연결 재수립 부하 테스트

## 8. 미결/보류 항목

- v2 프로토콜(seq/ack/resync) 및 클라이언트 개편 — Phase 4
- 온라인 상태·타이핑 — 제품 요구 발생 시 (세션 레지스트리 있어 증분 비용 낮음)
- 메시지 검색/통계 — 필요 시 DynamoDB → OpenSearch/분석 파이프라인 추가
- ElastiCache 구성 상세(클러스터 모드, 노드 크기) — 부하 테스트 후 확정
