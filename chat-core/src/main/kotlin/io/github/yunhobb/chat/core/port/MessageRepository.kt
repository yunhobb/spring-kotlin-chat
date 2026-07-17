package io.github.yunhobb.chat.core.port

import io.github.yunhobb.chat.core.domain.Message
import io.github.yunhobb.chat.core.domain.RoomId

/** 구현: storage-dynamo. 기존 DynamoDB 테이블이 source of truth다. */
interface MessageRepository {
    fun append(message: Message)

    fun findByRoom(roomId: RoomId, limit: Int, beforeMessageId: String? = null): List<Message>
}
