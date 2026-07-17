package io.github.yunhobb.chat.core.domain

import java.time.Instant

/**
 * TODO(선행 조사 docs/architecture.md §6-2): 기존 DynamoDB 스키마 문서화 후 필드·타입을 실제 계약에 맞춘다.
 *  messageId의 채번 방식(타임스탬프? 시퀀스?)도 기존 데이터와 호환돼야 한다.
 */
data class Message(
    val roomId: RoomId,
    val messageId: String,
    val senderSeq: Long,
    val type: MessageType,
    val payload: String,
    val sentAt: Instant,
)

/**
 * 레거시 API 관측(docs/legacy-api.md)에서 확인된 messageType 값. 계약상 이 문자열을 그대로 쓴다.
 * 이미지 등 미관측 타입이 더 있을 수 있어 §6-2에서 전수 확인 필요.
 * 직렬화 문자열이 소문자 카멜이므로 어댑터에서 매핑한다(enum 이름과 와이어 값 분리).
 */
enum class MessageType(val wireValue: String) {
    TEXT("textMessage"),
    PRODUCT("productMessage"),
    NOTICE("noticeMessage"); // 시스템/공지 — 사기 감지 배너 등

    companion object {
        fun fromWire(value: String): MessageType? = entries.find { it.wireValue == value }
    }
}
