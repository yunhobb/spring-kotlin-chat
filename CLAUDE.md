# 프로젝트 규칙 (Kotlin + Spring 컨벤션)

마켓플레이스 채팅 서버 재구축(strangler-fig 서버 교체). 배경·결정은 [DESIGN.md](DESIGN.md), 레거시 계약은 [LEGACY_API.md](LEGACY_API.md).

> 이 파일은 세션마다 자동 로드된다. 새 코드는 아래 규칙을 따르고, 규칙과 어긋나는 기존 코드를 만지면 규칙 쪽으로 맞춘다.

## 스택 (버전은 `gradle/libs.versions.toml` 단일 소스)

- Kotlin 2.4.10 · Spring Boot 4.1.0 · JDK 21(가상 스레드) · AWS SDK v2 2.46.7 · Kotest 6.2.2 + JUnit5
- **MVC + 가상 스레드** 확정. WebFlux 전면 도입 금지 — 코루틴/`suspend`/Reactor 도입하지 말 것. `ws-gateway`만 향후 Netty로 분리 가능하게 격리(§4).
- **JPA 금지**. 저장소는 DynamoDB(AWS SDK Enhanced Client, **동기**). 블로킹 IO는 가상 스레드가 흡수한다.

## 아키텍처 규칙 (헥사고날 — 어기면 안 되는 것)

- 의존 방향: `chat-api`·`ws-gateway` → **`chat-core`** ← `storage-dynamo`·`fanout`·`async-sqs`·`integration`.
- **`chat-core`는 프레임워크·AWS SDK 무의존**을 유지한다. Spring/Jackson/AWS import 금지. port 인터페이스만 소유하고, 유스케이스 빈 조립은 `app/config/CoreConfig`에서 한다.
- **어댑터 모듈끼리 직접 참조 금지.** 특히 `ws-gateway`는 `chat-core`의 port만 본다(Netty 분리 가능성 보존). fanout→ws-gateway가 필요하면 core에 inbound port를 추가해 방향을 유지한다.
- 관심사 분리는 새 모듈보다 **패키지**를 먼저 쓴다. 새 Gradle 모듈은 독립 배포·빌드 격리·팀 경계 중 하나가 실제로 생길 때만.

## Kotlin 관용 (베스트 프랙티스)

- **생성자 주입 + `private val`**, `@Autowired` 생략(단일 생성자면 자동). 필드 주입 금지.
- `val` 기본, `var`는 정당화될 때만. 불변 우선.
- **null-safety**: nullable(`String?`)을 명시적으로. `!!` 지양. 플랫폼 타입은 경계에서 즉시 좁힌다.
- DTO·설정·도메인 모델은 **data class**. `@ConfigurationProperties`는 생성자 바인딩 + `val`.
- `@Value`의 `$`는 Kotlin 문자열 템플릿과 충돌하므로 `@Value("\${prop}")`로 이스케이프.
- **value class는 도메인 안에서만.** Spring 바인딩 경계(`@PathVariable`, Jackson 직렬화, 설정)에서는 지원이 제한적이다. 예: `RoomId`(value class)는 chat-core 내부에서만 쓰고, REST/WS 경계에서는 `String`으로 받아 어댑터에서 `RoomId`로 변환한다.

## Spring / 빌드 컨벤션

- Spring 모듈은 `kotlin-spring` 플러그인(all-open)으로 프록시 대상 클래스를 자동 open. **chat-core에는 붙이지 않는다**(Spring 빈 없음).
- 컴파일러 플래그(모든 모듈): `-Xjsr305=strict`, `-Xannotation-default-target=param-property`(Spring Boot 4/Kotlin 2.2+ 권장).
- 부트스트랩은 `runApplication<App>(*args)` 관용을 쓴다.
- 의존성은 반드시 버전 카탈로그(`libs.versions.toml`)를 경유. 빌드 파일에 하드코딩된 버전 금지.

## 테스트 (TDD)

- **red → green**: 실패하는 테스트를 먼저 쓰고 최소 구현으로 통과시킨다.
- Kotest `StringSpec` + `shouldBe`/`shouldThrow` 매처. 순수 도메인은 손으로 짠 fake 포트로 검증(mocking 프레임워크 불필요).
- Spring 타입(`WebSocketSession` 등) 목이 필요하면 **MockK**(+ 필요시 SpringMockK). JUnit5 플랫폼 위에서 실행.
- 새 프로덕션 로직은 뮤테이션 관점에서 "테스트가 이 로직을 실제로 잡는가"를 확인한다.
- 검증 예: 도메인 경계값(inclusive/exclusive), 온·오프라인 분기, fail-fast 입력 검증.

## 프로젝트 불변식 (절대 깨지 말 것)

- **계약 유지**: 기존 앱 무수정이 원칙. `/api/chat/*` 경로·응답 봉투(`{meta,data}`)·`roomId={storeSeq}_{storeSeq}`·WS 프레임 프로토콜을 임의로 바꾸지 않는다. 스키마는 LEGACY_API.md 기준.
- **읽음 모델**: per-message 플래그가 아니라 멤버별 `lastReadAt`(epoch millis) 워터마크(`ReadModel`).
- **실시간은 best-effort, 정합성은 DB**: Redis 팬아웃 유실은 DB + 클라 재조회로 복구. 실시간 홉을 신뢰성 채널로 취급하지 말 것.
- **탈식별화 유지**: 패키지 `io.github.yunhobb.chat`, 문서 호스트명 `example.com`. 특정 회사 브랜드(한글/로마자) 워딩을 코드·문서·커밋에 다시 넣지 않는다.
- 선행 조사(§6: WS 프레임 규격, DynamoDB 스키마, 구 팬아웃 구조) 미확보 영역은 `TODO(...)` 스텁으로 두고 추측 구현하지 않는다.

## 커밋

- 한 커밋 = 한 관심사. 메시지는 한국어, 명령형 요약 + 근거 본문.
- 커밋/푸시는 사용자가 요청할 때만. 기본 브랜치에서 작업하면 먼저 브랜치를 판다.
