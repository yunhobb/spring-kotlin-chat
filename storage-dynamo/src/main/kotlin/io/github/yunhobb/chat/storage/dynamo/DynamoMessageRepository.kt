package io.github.yunhobb.chat.storage.dynamo

import io.github.yunhobb.chat.core.domain.Message
import io.github.yunhobb.chat.core.domain.RoomId
import io.github.yunhobb.chat.core.port.MessageRepository
import org.springframework.stereotype.Repository
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedClient

/**
 * 기존 테이블을 그대로 사용한다 — 스키마를 새로 설계하지 않는다 (서버 교체 원칙).
 * TODO(선행 조사 §6-2): 테이블명, PK/SK 구조, GSI, 구 서버만 아는 숨은 필드 문서화 후 매핑.
 */
@Repository
class DynamoMessageRepository(
    private val enhanced: DynamoDbEnhancedClient,
) : MessageRepository {

    override fun append(message: Message) {
        TODO("기존 메시지 테이블 스키마 문서화 후 구현")
    }

    override fun findByRoom(roomId: RoomId, limit: Int, beforeMessageId: String?): List<Message> {
        TODO("기존 메시지 테이블 스키마 문서화 후 구현")
    }
}
