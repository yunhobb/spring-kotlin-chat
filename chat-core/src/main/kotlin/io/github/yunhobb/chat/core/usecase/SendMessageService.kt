package io.github.yunhobb.chat.core.usecase

import io.github.yunhobb.chat.core.domain.Message
import io.github.yunhobb.chat.core.port.FanoutPublisher
import io.github.yunhobb.chat.core.port.MessageRepository
import io.github.yunhobb.chat.core.port.OfflinePushQueue
import io.github.yunhobb.chat.core.port.SessionRegistry

/**
 * 전송 흐름 (docs/architecture.md §3): 저장(source of truth) → 온라인 파드로 팬아웃 → 오프라인이면 푸시 큐.
 * 빈 등록은 app 모듈 CoreConfig에서 한다 — 이 모듈은 Spring을 모른다.
 */
class SendMessageService(
    private val messages: MessageRepository,
    private val sessions: SessionRegistry,
    private val fanout: FanoutPublisher,
    private val offlinePush: OfflinePushQueue,
) {
    fun send(message: Message) {
        messages.append(message)

        val (a, b) = message.roomId.participants
        val recipients = listOf(a, b).filter { it != message.senderSeq }

        for (recipient in recipients) {
            val locations = sessions.locate(recipient)
            if (locations.isEmpty()) {
                offlinePush.enqueue(recipient, message)
            } else {
                locations.forEach { fanout.publish(it, message) }
            }
        }
        // TODO(컷오버 Phase 2, docs/architecture.md §5): 신구 팬아웃 브리지 — 구 시스템 알림 채널에도 발행.
        //  구 시스템 팬아웃 구조 조사(§6-3) 후 이 지점에 브리지 port를 추가한다.
    }
}
