# chat-spring-kotlin

마켓플레이스 채팅 서버 재구축 (strangler-fig 서버 교체). 설계·조사·결정 문서는 **[docs/](docs/README.md)** 참조, 프로젝트 규칙은 **[CLAUDE.md](CLAUDE.md)**.

## 요구 사항

- JDK 21 (`brew install openjdk@21`)
- 빌드: `./gradlew build`

## 모듈 구조

| 모듈 | 역할 | 의존 |
|---|---|---|
| `app` | Spring Boot 조립·설정·빈 와이어링 | 전체 |
| `chat-core` | 도메인 + 유스케이스 + port. **프레임워크 무의존** | 없음 |
| `chat-api` | 기존 `/api/chat/*` REST 계약 구현 | core |
| `ws-gateway` | WS 핸드셰이크·레거시 프레임 코덱·로컬 세션맵. 분리 1순위 | core (port만) |
| `fanout` | Redis Pub/Sub 팬아웃 + 세션 레지스트리 | core |
| `storage-dynamo` | 기존 DynamoDB 테이블 어댑터 | core |
| `async-sqs` | SQS 비동기 경로(푸시·사기 감지) | core |
| `integration` | 타 서비스 연동 anti-corruption layer | core |

의존 규칙(헥사고날): 어댑터 모듈은 `chat-core`만 바라본다. 어댑터끼리 직접 참조 금지.

## 현재 상태

chat-core 도메인 4단위 TDD 완료. 선행 조사([docs/architecture.md](docs/architecture.md) §6: 레거시 WS 프레임 규격,
DynamoDB 스키마, 구 팬아웃 구조)가 끝나기 전까지 어댑터 구현부는 `TODO(...)` 스텁이다.
