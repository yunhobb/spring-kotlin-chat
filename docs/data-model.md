# 데이터 모델 설계 (DynamoDB)

> 저장소는 **기존 DynamoDB 테이블을 그대로 재사용**한다(서버 교체, 신규 스키마 설계 아님). 이 문서는 [legacy-api.md](legacy-api.md) 관측에서 **역추론한 스키마 가설**과 확정 필요 항목을 정리한다.
> ⚠️ **가설은 구현 근거가 아니다.** 실제 테이블 정의(§6-2)를 확보하기 전까지 리포지토리는 `TODO` 스텁으로 둔다(CLAUDE.md 불변식).

## 1. 관측에서 확인된 사실 (legacy-api.md)

- `roomId = "{storeSeqA}_{storeSeqB}"` — 유저쌍 문자열. **순서 규칙(정렬 canonical 여부) 미확정.**
- `messageId` = **number**, 방/전역 시퀀스로 추정.
- 타임스탬프 전부 epoch millis(number): createdAt, lastReadAt, frozenAt, fixedAt, deletedAt.
- 읽음 = per-message 플래그가 아니라 **멤버별 `lastReadAt` 워터마크**.
- 방 응답에 `unreadMessageCount`가 이미 있음 → **안읽음 수가 어딘가 집계·저장**돼 있다(파생 재계산이 아닐 수 있음).
- messageType: `textMessage` / `productMessage` / `noticeMessage`.

## 2. 스키마 가설 (확정 아님 — 검증 대상)

단일 테이블 설계로 추정. 아래는 **가설**이며 실제와 다를 수 있다.

```
# 메시지 아이템 (가설)
PK = ROOM#{roomId}
SK = MSG#{messageId(zero-padded)}      # 시간순 정렬
attrs: senderSeq, messageType, message, data(map), createdAt

# 방 메타 아이템 (가설)
PK = ROOM#{roomId}
SK = META
attrs: productSeq, memberStoreSeqList, createdAt, lastMessage...

# 멤버 아이템 (가설 — 읽음 워터마크)
PK = ROOM#{roomId}
SK = MEMBER#{storeSeq}
attrs: lastReadAt, role, statusCode

# 유저→방 목록 조회용 GSI (가설)
GSI1PK = USER#{storeSeq}
GSI1SK = {lastMessageAt}                # 최근 대화순 정렬
```

- **seq 채번**: 방 메타 아이템에 원자 카운터(`ADD messageId 1`) 후 그 값을 SK로 쓰는 패턴이 유력하나 **미확인**.

## 3. 확정 필요 항목 (§6-2)

| # | 항목 | 왜 필요한가 |
|---|---|---|
| 1 | 실제 테이블명·PK/SK 명명 | 매핑의 출발점 |
| 2 | roomId storeSeq 순서 규칙 | `RoomId.of` canonical 정렬 여부([legacy-api.md] roomId) |
| 3 | messageId 채번 규칙 | 원자 카운터? 클라 생성? seq 정렬성 보장 방식 |
| 4 | GSI 구성(방 목록·상품별 chat-count) | `findRoomsOf`, `chat-count` 쿼리 |
| 5 | unreadMessageCount 저장 위치 | 실시간 갱신 대상인지, 파생 계산인지 |
| 6 | 용량 모드(온디맨드/프로비저닝), TTL 속성 | 운영·비용·비활성 방 정리 |
| 7 | 구 서버만 쓰는 숨은 필드 | 신 서버가 안 쓰면 데이터 유실 위험 |

## 4. 도메인 매핑 방침

- **JPA 불사용.** AWS SDK v2 **Enhanced Client**(동기)로 테이블 스키마 ↔ 코틀린 객체 매핑. 블로킹 IO는 가상 스레드가 흡수.
- 도메인(`chat-core`)은 AWS 무의존. DynamoDB 애노테이션/타입은 `storage-dynamo` 어댑터의 별도 아이템 클래스에만 두고, 도메인 ↔ 아이템 변환을 어댑터에서 수행.
- 읽음은 도메인 `ReadModel`(멤버 `lastReadAt` 워터마크 비교)로 계산 — 이미 TDD 구현됨.

## 5. 로컬 테스트 전략

`storage-dynamo`의 `AwsProperties.endpointOverride`로 **DynamoDB Local**을 물려 통합 테스트한다(코드 변경 불필요).

```bash
docker run -p 8000:8000 amazon/dynamodb-local
# aws.endpoint-override=http://localhost:8000
```

스키마 확정 후: DynamoDB Local에 그 스키마로 테이블 생성 → 리포지토리 TDD(append/findByRoom/unreadCount). 상세는 [testing-strategy.md](testing-strategy.md).
