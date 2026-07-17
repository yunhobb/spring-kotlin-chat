package io.github.yunhobb.chat.core.usecase

import io.github.yunhobb.chat.core.domain.Message
import io.github.yunhobb.chat.core.domain.MessageType
import io.github.yunhobb.chat.core.domain.RoomId
import io.github.yunhobb.chat.core.port.FanoutPublisher
import io.github.yunhobb.chat.core.port.MessageRepository
import io.github.yunhobb.chat.core.port.OfflinePushQueue
import io.github.yunhobb.chat.core.port.SessionLocation
import io.github.yunhobb.chat.core.port.SessionRegistry
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import java.time.Instant

class SendMessageServiceTest : StringSpec({

    val room = RoomId("100_200")
    fun message(sender: Long) =
        Message(room, "m1", sender, MessageType.TEXT, "안녕", Instant.parse("2026-07-17T00:00:00Z"))

    // --- 손으로 짠 fake 포트들 (mocking 프레임워크 불필요) ---
    class Fakes {
        val events = mutableListOf<String>()
        val appended = mutableListOf<Message>()
        val published = mutableListOf<Pair<SessionLocation, Message>>()
        val pushed = mutableListOf<Pair<Long, Message>>()
        var locations: Map<Long, List<SessionLocation>> = emptyMap()

        val messages = object : MessageRepository {
            override fun append(message: Message) { appended += message; events += "append" }
            override fun findByRoom(roomId: RoomId, limit: Int, beforeMessageId: String?) = emptyList<Message>()
        }
        val sessions = object : SessionRegistry {
            override fun register(storeSeq: Long, location: SessionLocation) {}
            override fun unregister(storeSeq: Long, location: SessionLocation) {}
            override fun locate(storeSeq: Long) = locations[storeSeq] ?: emptyList()
        }
        val fanout = object : FanoutPublisher {
            override fun publish(target: SessionLocation, message: Message) {
                published += target to message; events += "publish"
            }
        }
        val offline = object : OfflinePushQueue {
            override fun enqueue(recipientSeq: Long, message: Message) {
                pushed += recipientSeq to message; events += "push"
            }
        }
        val service = SendMessageService(messages, sessions, fanout, offline)
    }

    "메시지를 저장소에 먼저 저장한다 (source of truth 우선)" {
        val f = Fakes()
        f.locations = mapOf(200L to listOf(SessionLocation("pod-a")))
        f.service.send(message(sender = 100))
        f.appended.map { it.messageId } shouldContainExactly listOf("m1")
        f.events.first() shouldBe "append" // 팬아웃보다 먼저
    }

    "수신자가 온라인이면 그의 모든 파드로 팬아웃하고 푸시는 하지 않는다" {
        val f = Fakes()
        f.locations = mapOf(200L to listOf(SessionLocation("pod-a"), SessionLocation("pod-b")))
        f.service.send(message(sender = 100))
        f.published.map { it.first.podId } shouldContainExactly listOf("pod-a", "pod-b")
        f.pushed.shouldBeEmpty()
    }

    "수신자가 오프라인이면 푸시 큐로 보내고 팬아웃은 하지 않는다" {
        val f = Fakes()
        f.locations = emptyMap() // 200 오프라인
        f.service.send(message(sender = 100))
        f.pushed.map { it.first } shouldContainExactly listOf(200L)
        f.published.shouldBeEmpty()
    }

    "발신자 본인은 수신 대상에서 제외한다" {
        val f = Fakes()
        f.locations = mapOf(
            100L to listOf(SessionLocation("pod-sender")),
            200L to listOf(SessionLocation("pod-a")),
        )
        f.service.send(message(sender = 100))
        // 발신자(100)의 파드로는 팬아웃하지 않는다
        f.published.map { it.first.podId } shouldContainExactly listOf("pod-a")
    }
})
