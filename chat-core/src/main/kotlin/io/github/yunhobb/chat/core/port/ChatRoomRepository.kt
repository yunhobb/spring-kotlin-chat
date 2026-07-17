package io.github.yunhobb.chat.core.port

import io.github.yunhobb.chat.core.domain.ChatRoom
import io.github.yunhobb.chat.core.domain.RoomId

/** 구현: storage-dynamo. */
interface ChatRoomRepository {
    fun find(roomId: RoomId): ChatRoom?

    fun findRoomsOf(storeSeq: Long): List<ChatRoom>

    fun unreadCountOf(storeSeq: Long): Long
}
