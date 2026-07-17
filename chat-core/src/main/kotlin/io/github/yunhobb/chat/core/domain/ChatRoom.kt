package io.github.yunhobb.chat.core.domain

import java.time.Instant

/**
 * TODO(선행 조사 DESIGN.md §6-2): 기존 테이블 확인 후 확정.
 *  실사 기준으로 방에는 상품 컨텍스트(상품 카드·구매하기)와 안읽음 상태가 붙는다.
 */
data class ChatRoom(
    val roomId: RoomId,
    val productSeq: Long?,
    val lastMessageAt: Instant?,
)
