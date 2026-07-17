package io.github.yunhobb.chat.core.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

class RoomIdTest : StringSpec({

    "유효한 {seq}_{seq} 형식을 받아들이고 참여자를 파싱한다" {
        val room = RoomId("5062595_3276169")
        room.participants shouldBe (5062595L to 3276169L)
    }

    "언더스코어가 없으면 거부한다" {
        shouldThrow<IllegalArgumentException> { RoomId("5062595") }
    }

    "숫자가 아닌 값을 거부한다" {
        shouldThrow<IllegalArgumentException> { RoomId("abc_123") }
    }

    "참여자가 3개 이상이면 거부한다" {
        shouldThrow<IllegalArgumentException> { RoomId("1_2_3") }
    }

    "빈 문자열을 거부한다" {
        shouldThrow<IllegalArgumentException> { RoomId("") }
    }

    "contains — storeSeq가 참여자인지 판정한다" {
        val room = RoomId("100_200")
        room.contains(100L) shouldBe true
        room.contains(200L) shouldBe true
        room.contains(999L) shouldBe false
    }

    "otherParticipant — 나를 제외한 상대 storeSeq를 반환한다" {
        val room = RoomId("100_200")
        room.otherParticipant(100L) shouldBe 200L
        room.otherParticipant(200L) shouldBe 100L
    }

    "otherParticipant — 참여자가 아닌 storeSeq면 예외" {
        val room = RoomId("100_200")
        shouldThrow<IllegalArgumentException> { room.otherParticipant(999L) }
    }

    "of는 인자 순서를 보존한다 (canonical 정렬 규칙은 §6-2 미확정)" {
        // 순서 규칙이 확정되기 전까지 of는 준 순서를 그대로 유지해야 한다.
        RoomId.of(100L, 200L).value shouldBe "100_200"
        RoomId.of(200L, 100L).value shouldBe "200_100"
    }

    "toString은 원본 값과 같다" {
        RoomId("100_200").toString() shouldBe "100_200"
    }
})
