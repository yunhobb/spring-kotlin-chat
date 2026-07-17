package io.github.yunhobb.chat.core.port

import io.github.yunhobb.chat.core.domain.Message

/** 구현: async-sqs. 유실 불가 작업(FCM/APNs 푸시)은 큐를 경유한다. */
interface OfflinePushQueue {
    fun enqueue(recipientSeq: Long, message: Message)
}
