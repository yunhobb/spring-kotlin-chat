package io.github.yunhobb.chat.core.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import java.time.Instant

/**
 * 레거시 읽음 모델(docs/legacy-api.md): per-message 플래그가 아니라 멤버별 lastReadAt(워터마크).
 * "읽음/안읽음"은 message.sentAt 과 상대 lastReadAt 비교로 계산한다. 워터마크는 포함(inclusive).
 */
class ReadModelTest : StringSpec({

    val roomId = RoomId("100_200")
    val t0 = Instant.parse("2026-07-17T00:00:00Z")
    fun at(seconds: Long) = t0.plusSeconds(seconds)
    fun msg(id: Long, sender: Long, sec: Long) =
        Message(roomId, "m$id", sender, MessageType.TEXT, "…", at(sec))

    "isReadBy — 워터마크보다 이전에 온 메시지는 읽음" {
        ReadModel.isReadBy(msg(1, 200, 10), readerLastReadAt = at(20)) shouldBe true
    }

    "isReadBy — 워터마크와 같은 시각이면 읽음 (inclusive)" {
        ReadModel.isReadBy(msg(1, 200, 20), readerLastReadAt = at(20)) shouldBe true
    }

    "isReadBy — 워터마크 이후 메시지는 안읽음" {
        ReadModel.isReadBy(msg(1, 200, 30), readerLastReadAt = at(20)) shouldBe false
    }

    "unreadCount — 상대가 보낸, 워터마크 이후 메시지만 센다" {
        val messages = listOf(
            msg(1, 200, 10), // 이전 → 읽음
            msg(2, 200, 25), // 이후 → 안읽음
            msg(3, 200, 30), // 이후 → 안읽음
        )
        ReadModel.unreadCount(messages, viewerSeq = 100, viewerLastReadAt = at(20)) shouldBe 2
    }

    "unreadCount — 내가 보낸 메시지는 이후여도 안읽음에 포함하지 않는다" {
        val messages = listOf(
            msg(1, 100, 25), // 내 메시지 → 제외
            msg(2, 200, 25), // 상대 → 안읽음
        )
        ReadModel.unreadCount(messages, viewerSeq = 100, viewerLastReadAt = at(20)) shouldBe 1
    }

    "unreadCount — 워터마크와 같은 시각의 메시지는 읽음으로 본다" {
        val messages = listOf(msg(1, 200, 20))
        ReadModel.unreadCount(messages, viewerSeq = 100, viewerLastReadAt = at(20)) shouldBe 0
    }

    "unreadCount — 빈 목록은 0" {
        ReadModel.unreadCount(emptyList(), viewerSeq = 100, viewerLastReadAt = at(20)) shouldBe 0
    }

    "unreadCount — 전부 읽었으면 0" {
        val messages = listOf(msg(1, 200, 5), msg(2, 200, 10))
        ReadModel.unreadCount(messages, viewerSeq = 100, viewerLastReadAt = at(20)) shouldBe 0
    }
})
