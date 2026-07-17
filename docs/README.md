# 문서 색인

마켓플레이스 채팅 서버 재구축 프로젝트의 설계·조사 문서. 프로젝트 규칙은 루트 [../CLAUDE.md](../CLAUDE.md).

## 설계

| 문서 | 내용 |
|---|---|
| [architecture.md](architecture.md) | 전체 아키텍처·확정 결정·컷오버 전략·EKS 운영 |
| [realtime-and-session-design.md](realtime-and-session-design.md) | WS·세션·팬아웃 계층 상세 설계 (다음 구현 대상) |
| [data-model.md](data-model.md) | DynamoDB 스키마 가설과 확정 필요 항목(§6-2) |
| [testing-strategy.md](testing-strategy.md) | 계층별 TDD 전략(Kotest/MockK/DynamoDB Local) |

## 조사

| 문서 | 내용 |
|---|---|
| [legacy-api.md](legacy-api.md) | 레거시 REST 계약 관측 결과(스키마만, 개인정보 제외) |

## 결정 기록 (ADR)

| # | 결정 |
|---|---|
| [0001](adr/0001-mvc-over-webflux.md) | WebFlux 대신 MVC + 가상 스레드 |
| [0002](adr/0002-modular-monolith.md) | 헥사고날 모듈러 모놀리스 |
| [0003](adr/0003-pure-websocket-protocol.md) | 순수 WebSocket + 기존 프레임 유지 |
| [0004](adr/0004-redis-fanout.md) | Redis Pub/Sub 팬아웃 + 세션 레지스트리 |
| [0005](adr/0005-dynamodb-no-jpa.md) | DynamoDB 유지 · JPA 불사용 |
| [0006](adr/0006-contract-preservation-strangler.md) | 계약 유지 + strangler-fig 컷오버 |

## 현재 상태

- 스캐폴딩 + chat-core 도메인 4단위 TDD 완료(RoomId, MessageType, ReadModel, SendMessageService).
- 어댑터(WS/DynamoDB/Redis/SQS)는 선행 조사(§6) 대기 스텁.
- **최대 블로커**: WS 프레임 규격(§6-1), DynamoDB 실제 스키마(§6-2).
