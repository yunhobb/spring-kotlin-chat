package io.github.yunhobb.chat.wsgateway

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.springframework.web.socket.WebSocketSession

/**
 * 설계: docs/realtime-and-session-design.md §3.
 * WebSocketSession은 인터페이스이므로 MockK로 서로 다른 인스턴스를 만든다(메서드 호출 없음 — 신원 비교만).
 */
class LocalSessionMapTest : StringSpec({

    fun session() = mockk<WebSocketSession>()

    "add한 세션은 sessionsOf에 담기고 isOnline이 true" {
        val map = LocalSessionMap()
        val s = session()
        map.add(100, s)
        map.sessionsOf(100) shouldContainExactly setOf(s)
        map.isOnline(100) shouldBe true
    }

    "멀티 디바이스 — 한 유저의 여러 세션이 공존한다" {
        val map = LocalSessionMap()
        val s1 = session()
        val s2 = session()
        map.add(100, s1)
        map.add(100, s2)
        map.sessionsOf(100) shouldContainExactlyInAnyOrder listOf(s1, s2)
        map.connectionCount() shouldBe 2
    }

    "세션 하나를 remove해도 나머지는 남는다" {
        val map = LocalSessionMap()
        val s1 = session()
        val s2 = session()
        map.add(100, s1)
        map.add(100, s2)
        map.remove(100, s1)
        map.sessionsOf(100) shouldContainExactly setOf(s2)
        map.isOnline(100) shouldBe true
    }

    "마지막 세션을 remove하면 오프라인이 되고 엔트리가 정리된다" {
        val map = LocalSessionMap()
        val s = session()
        map.add(100, s)
        map.remove(100, s)
        map.sessionsOf(100) shouldBe emptySet()
        map.isOnline(100) shouldBe false
        map.connectionCount() shouldBe 0
    }

    "connectionCount는 여러 유저에 걸쳐 합산한다" {
        val map = LocalSessionMap()
        map.add(100, session())
        map.add(100, session())
        map.add(200, session())
        map.connectionCount() shouldBe 3
    }

    "isOnline — 연결 없는 유저는 false" {
        val map = LocalSessionMap()
        map.isOnline(999) shouldBe false
    }

    "sessionsOf는 스냅샷을 반환한다 (이후 변경이 반영되지 않음)" {
        val map = LocalSessionMap()
        val s1 = session()
        map.add(100, s1)
        val snapshot = map.sessionsOf(100)
        map.add(100, session()) // 스냅샷 취득 후 추가
        snapshot shouldContainExactly setOf(s1)
    }

    "추가된 적 없는 세션을 remove해도 예외 없이 무시된다" {
        val map = LocalSessionMap()
        map.remove(100, session()) // 존재하지 않는 키
        map.add(100, session())
        map.remove(100, session()) // 존재하는 키, 다른 세션
        map.isOnline(100) shouldBe true
    }
})
