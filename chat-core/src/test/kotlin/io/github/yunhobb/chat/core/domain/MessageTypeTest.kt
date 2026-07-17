package io.github.yunhobb.chat.core.domain

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe

class MessageTypeTest : StringSpec({

    "관측된 wire 값을 enum으로 매핑한다 (LEGACY_API.md)" {
        MessageType.fromWire("textMessage") shouldBe MessageType.TEXT
        MessageType.fromWire("productMessage") shouldBe MessageType.PRODUCT
        MessageType.fromWire("noticeMessage") shouldBe MessageType.NOTICE
    }

    "미지의 wire 값은 null (모르는 타입에 예외 대신 관용적 처리)" {
        MessageType.fromWire("stickerMessage").shouldBeNull()
        MessageType.fromWire("").shouldBeNull()
    }

    "각 enum의 wireValue가 계약 문자열과 정확히 일치한다" {
        MessageType.TEXT.wireValue shouldBe "textMessage"
        MessageType.PRODUCT.wireValue shouldBe "productMessage"
        MessageType.NOTICE.wireValue shouldBe "noticeMessage"
    }

    "wireValue → fromWire 왕복이 항등이다" {
        MessageType.entries.forEach { type ->
            MessageType.fromWire(type.wireValue) shouldBe type
        }
    }

    "wire 값에 중복이 없다" {
        val wires = MessageType.entries.map { it.wireValue }
        wires.toSet().size shouldBe wires.size
    }
})
