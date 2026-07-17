# 레거시 채팅 API 관측 결과 (선행 조사 §6-1 부분 완료)

> 2026-07-17, 로그인된 본인 세션에서 `web.example.com` 채팅이 실제로 주고받는 트래픽을 관찰해 도출.
> **실제 대화 내용·상대 닉네임·프로필·토큰 등 개인정보는 기록하지 않는다.** 필드명과 타입(스키마)만 남긴다.
> 이 문서는 "계약 그대로 유지"(DESIGN.md §1-6) 구현의 기준 계약이다.

## 관측 방법·범위

- 브라우저 XHR을 계측해 응답을 값이 아닌 스키마(키→타입)로 변환. GET(읽기 전용)만 관측.
- **전송(POST/WS)은 실제 상대방에게 메시지가 나가므로 의도적으로 유발하지 않음** → 하단 "미확인" 참조.
- 호스트: 채팅 API 베이스 = `https://edge-live.example.com`
- 인증: JWT Bearer (사용자 확인). 앱/웹이 `Authorization` 헤더로 전달. 토큰 값은 기록하지 않음.

## 결정적 발견: 웹에는 실시간 채널이 없다

방 진입 시 REST로 메시지를 1회 로드할 뿐, 유휴 10초간 **WebSocket·SSE·폴링 전부 0회**.
→ 실시간 수신(WS 프레임 프로토콜)은 **앱 전용**이며 웹 관찰로는 복원 불가. 구 서버 소스 또는 앱 디컴파일 필요(§6-1 잔여).
번들에 `wss://socket…`, `EventSource`, `ably` 문자열이 있으나 웹 런타임에선 활성화되지 않음(앱 공유 코드 또는 사장 코드).

## 공통 응답 봉투

```jsonc
{
  "meta": { "code": number, "message": string },
  "data": { /* 엔드포인트별 */ }
}
```

- 모든 타임스탬프는 `number`(epoch millis)로 관측됨: createdAt, lastReadAt, frozenAt, fixedAt, deletedAt, displayFrom/To 등.
- `roomId` 형식: `"{storeSeqA}_{storeSeqB}"` (숫자쌍). 순서 규칙(정렬 canonical 여부)은 §6-2에서 데이터로 확정 필요.
- `messageId`: **number** (전역/방별 시퀀스로 추정). ← 우리 seq 설계에 직접 관련. 채번 규칙은 §6-2 확정 필요.

## 엔드포인트 (관측된 것)

### GET /api/chat/rooms?last_message_at=
방 목록. `last_message_at` = 커서 페이지네이션으로 추정.
```jsonc
data.roomList: array<Room>
```

### GET /api/chat/rooms/{roomId}
방 상세.
```jsonc
data: {
  roomId, roomType: string, badgeType: null,
  roomTitle: string, storeSeq: number, memberCount: number,
  memberStoreSeqList: array<number>,
  unreadMessageCount: number,
  isAlarmEnabled, isFixed, isFrozen: boolean,
  frozenAt, fixedAt, createdAt, deletedAt: number,
  blockList: array,           // 빈 배열만 관측 — 원소 스키마 미확인
  memberList: array<Member>,
  lastMessage: Message,        // 목록용, sender=null·data={} 로 축약 관측
  product: ProductSnapshot,
  featureDisplay: null
}
```

### GET /api/chat/rooms/{roomId}/messages?from=&to=
메시지 목록. `from`/`to` = messageId 범위 페이지네이션으로 추정(방향·포함성 §6-2 확정).
```jsonc
data.messageList: array<Message>
```

### GET /api/chat/users/{storeSeq}/region
상대 직거래 지역 경고 배너.
```jsonc
data: { isKorea: boolean, country: string, iconImageUrl: url,
        warningText: string, warningHighlightText: string, metaCode: number }
```

### GET /api/chat/quick-buttons
역할별 퀵버튼.
```jsonc
data: {
  seller: array<QuickButton>,
  buyer:  array<QuickButton>
}
QuickButton: { id: string, displayPriority: number, title: string,
               iconUrl: null, appLink: string, webLink: url,
               displayFrom: number, displayTo: number }
```

## 공통 오브젝트 스키마

### Member
```jsonc
{
  storeSeq, userSeq: number, nickName: string, profileImageUrl: url,
  statusCode: number, role: string, isSelf: boolean,
  lastReadAt: number,           // ★ 읽음은 per-message 플래그가 아니라 멤버별 lastReadAt 워터마크.
                                //    "읽음/안읽음"은 message.createdAt vs 상대 lastReadAt 비교로 계산.
  chatResponseRatio: number, chatResponseTime: number, chatResponseTimeText: string,
  blockingMe: boolean, isBlocked: boolean
}
```

### Message (봉투)
```jsonc
{
  messageId: number, createdAt: number, roomId: string,
  messageType: string,          // 아래 enum
  message: string,              // 표시용 텍스트(타입별 의미 상이)
  sender: { storeSeq, userSeq: number, nickName: string, profileImageUrl: url } | null,
  data: { messageType: string, ... }   // 타입별 페이로드. 봉투와 data 양쪽에 messageType 중복 존재
}
```

### messageType 값과 data 페이로드 (관측된 3종)
```jsonc
// textMessage
data: { messageType, originMessage: string }

// productMessage  (상품 카드)
data: { messageType, price: number, productSeq: number, title: string,
        imageUrl: url, sellerStoreSeq: number, platformType: number, cafeArticleId: string }

// noticeMessage  (시스템/공지 — 사기 감지 배너 등, 리치 렌더링)
data: {
  messageType, notificationType: string, displayType: string,
  title: string, backgroundColor, borderColor: string,
  receiverUserIds: array<string(numeric)>,
  iconImageUrl: url, isTimeDisplay: boolean,
  displaySort: array<string>,
  badges: { badgeSeq: number, badgeName: string,
            backgroundColor, borderColor, fontColor: string, notificationCodeId: null },
  contents: array<{ contentText: string }>
}
```
> 관측되지 않은 타입(이미지 등)이 더 있을 수 있음 — §6-2에서 전수 확인.

### ProductSnapshot (roomList item.product)
```jsonc
{ productSeq: number, thumbnailUrl: url,
  storeSeq, productTitle, productPrice, parcelFeeYn, productStatus,
  productHiddenStatus, platformType, cafeArticleId, order, button, categorySeq: null }
// null 필드들은 목록 응답에서 미포함일 뿐, 상세/다른 경로에서 채워질 수 있음
```

## 미확인 — 앱/구 서버 소스 필요 (§6 잔여 과제)

| 항목 | 이유 |
|---|---|
| **메시지 전송 방식** (POST 경로 or WS) | 실제 발송을 피함. 앱은 WS로 보낼 가능성 큼 |
| **WS 프레임 프로토콜** | 웹에 실시간 채널 없음. 번들에 TALK/ack/sync 키워드만 확인 |
| 실시간 수신(신규 메시지 push) | 웹은 재조회 방식. 앱 전용 |
| `/emoticons` 실제 경로·스키마 | 웹 UI에서 미유발(상대 탈퇴 방이라 입력 불가) |
| `/products/{productSeq}/chat-count` | 상품 페이지에서만 호출 |
| `/users/{storeSeq}/unread-message-cnt` 스키마 | 초기 로드 시 호출됨. 값은 unread 카운트 |
| `POST /users/{s}/block-user/{b}` 요청/응답 | 상태 변경이라 미유발 |
| `from`/`to`, `last_message_at` 페이지네이션 정확한 의미 | 경계·정렬 방향 데이터 확인 필요 |
| roomId storeSeq 순서 규칙, messageId 채번 규칙 | §6-2 DynamoDB 데이터로 확정 |

## 설계 반영 포인트 (DESIGN.md 갱신 대상)

1. **읽음 모델**: per-message 플래그가 아니라 **멤버별 `lastReadAt`(epoch millis) 워터마크**. 우리 `chat-core`의 읽음 설계를 seq 기반이 아니라 이 워터마크 계약에 맞춰야 함(계약 유지).
2. **messageId가 number 시퀀스**: v2 seq/ack/resync 설계 시 재활용 가능.
3. **noticeMessage = 사기 감지·시스템 메시지 채널**: async-sqs 사기 감지 파이프라인 출력이 이 타입으로 방에 주입됨.
4. **productMessage**: integration 모듈(상품 서비스)과 결합 지점.
5. 응답 봉투 `{meta, data}`를 chat-api 전 응답에 공통 적용.
