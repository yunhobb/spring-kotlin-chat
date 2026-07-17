# 테스트 전략

> 도구·관례는 [../CLAUDE.md](../CLAUDE.md) "테스트" 절이 규범. 이 문서는 계층별 전략과 근거를 정리한다.

## 1. 원칙

- **TDD (red → green)**: 실패 테스트를 먼저 쓰고 최소 구현으로 통과. 리팩터는 green 유지하며.
- **테스트가 실제 로직을 잡는지 검증**: 새 프로덕션 로직은 뮤테이션 관점으로 점검한다(로직을 일부러 반전 → 테스트가 RED 되는지).
- **인프라 없이 도는 순수 테스트 우선**. 외부 의존이 필요한 것만 통합 테스트로 격리.

## 2. 도구

| 용도 | 선택 | 비고 |
|---|---|---|
| 테스트 문법·매처 | **Kotest** 6.2.2 (`StringSpec`, `shouldBe`/`shouldThrow`) | 실행 플랫폼은 JUnit5 |
| 실행 플랫폼 | JUnit5 | Gradle `useJUnitPlatform()` |
| 목(mock) | **MockK** | Spring 타입(`WebSocketSession` 등) 대체. 순수 도메인은 목 대신 손수 짠 fake |
| DynamoDB 통합 | **DynamoDB Local**(docker) | `AwsProperties.endpointOverride`로 연결 |

## 3. 계층별 전략

### chat-core (순수 도메인) — 최우선, 인프라 0
- 손으로 짠 **fake 포트**로 유스케이스 검증(mocking 프레임워크 불필요).
- 완료: `RoomId`(10), `MessageType`(5), `ReadModel`(8), `SendMessageService`(4) — 총 29 테스트, 뮤테이션 3건·엣지 프로브로 견고성 확인.
- 도메인 경계값(inclusive/exclusive), 분기(온·오프라인), fail-fast 입력 검증을 반드시 포함.

### ws-gateway — MockK 도입 지점
- `LocalSessionMap`: `mockk<WebSocketSession>()`로 세션 생성. add/remove/멀티디바이스/cleanup/`isOnline`/스냅샷/동시성([realtime-and-session-design.md](realtime-and-session-design.md) §3) 검증.
- `LegacyFrameCodec`/핸들러: **프레임 규격(§6-1) 확보 전 구현·테스트 불가** → 스텁 유지.

### fanout — 임베디드/통합
- `RedisSessionRegistry`/`RedisFanoutPublisher`: 키 구조·TTL·라우팅. 임베디드 Redis 또는 Testcontainers.
- 페이로드 직렬화 포맷 확정 후 라운드트립 테스트.

### storage-dynamo — DynamoDB Local 통합
- **스키마 확정(§6-2) 후** 리포지토리 TDD: `append` → `findByRoom` 라운드트립, seq 정렬, `unreadCount` 워터마크.
- 확정 전 스텁 유지(추측 매핑 금지).

### chat-api — MockMvc + 계약
- 응답 봉투 `{meta,data}`·DTO 직렬화가 [legacy-api.md](legacy-api.md) 스키마와 일치하는지. 서비스는 fake로 주입.

### app — 스모크
- `contextLoads`로 전 모듈 빈 조립 검증(외부 인프라 미연결). 현재 통과.

## 4. 통합 테스트 격리

- 단위(순수) 테스트는 `test`, 인프라 필요한 통합 테스트는 태그/소스셋 분리 예정(Testcontainers 기동 비용). CI에서 단위→통합 순 실행.
