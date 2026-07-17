package io.github.yunhobb.chat.core.port

import io.github.yunhobb.chat.core.domain.Message

/**
 * 구현: fanout(Redis Pub/Sub). 실시간 홉은 best-effort이며 유실 시 정합성은
 * DB(MessageRepository)와 클라이언트 재조회가 보장한다 (docs/architecture.md §3).
 */
interface FanoutPublisher {
    fun publish(target: SessionLocation, message: Message)
}
