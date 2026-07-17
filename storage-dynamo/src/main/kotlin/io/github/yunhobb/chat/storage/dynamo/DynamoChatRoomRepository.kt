package io.github.yunhobb.chat.storage.dynamo

import io.github.yunhobb.chat.core.domain.ChatRoom
import io.github.yunhobb.chat.core.domain.RoomId
import io.github.yunhobb.chat.core.port.ChatRoomRepository
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient

@Repository
class DynamoChatRoomRepository(
    private val enhanced: DynamoDbEnhancedClient,
) : ChatRoomRepository {

    override fun find(roomId: RoomId): ChatRoom? {
        TODO("기존 방 테이블 스키마 문서화(§6-2) 후 구현")
    }

    override fun findRoomsOf(storeSeq: Long): List<ChatRoom> {
        TODO("기존 방 목록 조회 패턴(GSI) 문서화 후 구현")
    }

    override fun unreadCountOf(storeSeq: Long): Long {
        TODO("기존 안읽음 카운트 저장 방식 문서화 후 구현")
    }
}
