package io.github.yunhobb.chat.core.domain

import java.time.Instant

/**
 * 읽음 상태 계산 (LEGACY_API.md 발견 반영).
 *
 * 레거시는 메시지별 읽음 플래그가 없다. 각 멤버의 lastReadAt(워터마크)만 저장하고,
 * 특정 메시지가 읽혔는지는 message.sentAt 과 워터마크를 비교해 파생한다.
 * 워터마크는 포함(inclusive): sentAt == lastReadAt 이면 읽은 것으로 본다.
 */
object ReadModel {

    /** [message]가 [readerLastReadAt] 워터마크를 가진 사용자에게 읽혔는가. */
    fun isReadBy(message: Message, readerLastReadAt: Instant): Boolean =
        !message.sentAt.isAfter(readerLastReadAt)

    /**
     * [viewerSeq] 관점의 안읽음 메시지 수 = 상대가 보낸 메시지 중 워터마크 이후에 온 것.
     * 본인이 보낸 메시지는 세지 않는다.
     */
    fun unreadCount(messages: List<Message>, viewerSeq: Long, viewerLastReadAt: Instant): Int =
        messages.count { it.senderSeq != viewerSeq && it.sentAt.isAfter(viewerLastReadAt) }
}
