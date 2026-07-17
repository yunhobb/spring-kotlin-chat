# ADR-0006: 계약 유지 + strangler-fig 컷오버

- 상태: 확정 (2026-07-17)
- 맥락: 데이터 마이그레이션이 아니라 **서버 구현만 교체**. 앱 강제 업데이트 없이.

## 결정
기존 **API 계약과 데이터를 그대로 두고 서버 구현만** Kotlin Spring으로 교체한다(strangler-fig). 신구 서버가 같은 DynamoDB를 본다.

## 유지해야 하는 계약 (깨면 안 됨)
- REST 경로 `/api/chat/*`, 응답 봉투 `{meta, data}`, 스키마 → [../legacy-api.md](../legacy-api.md).
- `roomId = {storeSeq}_{storeSeq}`, `messageId` number 시퀀스.
- 읽음 = 멤버별 `lastReadAt`(epoch millis) 워터마크.
- WS 프레임 프로토콜([ADR-0003](0003-pure-websocket-protocol.md)).

## 컷오버 단계
1. 선행 조사(§6) — WS 프레임·DynamoDB 스키마·구 팬아웃 구조.
2. 읽기 카나리 — rooms/messages/unread를 ALB 가중치로 점진 전환.
3. 쓰기+WS 카나리 — **신구 팬아웃 브리지**로 신구 걸친 유저 간 실시간 보장.
4. 100% 전환 — 구 서버·브리지 제거.
5. (선택 v2) seq/ack/resync 프로토콜, ws-gateway Netty 분리.

## 파생 규칙
- 미확보 영역은 추측 구현 금지 → `TODO` 스텁.
- 브랜드 탈식별화 유지(코드·문서·커밋에 특정 회사 워딩 금지).
