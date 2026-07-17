package io.github.yunhobb.chat.core.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec

/**
 * 회귀 방지: 형식 검증(정규식)과 참여자 파싱(toLong)의 불일치 재발 차단.
 * 이전에는 Long 범위를 넘는 숫자 문자열이 정규식 `\d+_\d+` 를 통과해 생성되고,
 * participants 접근 시점에 NumberFormatException 으로 지연 폭발했다.
 * 이제는 생성 시점에 fail-fast(IllegalArgumentException) 해야 한다.
 */
class RoomIdEdgeProbeTest : StringSpec({

    "Long 범위를 넘는 storeSeq는 생성 시점에 거부한다 (지연 폭발 금지)" {
        val huge = "99999999999999999999_1" // 20자리 > Long.MAX(19자리)
        shouldThrow<IllegalArgumentException> { RoomId(huge) }
    }

    "양쪽 모두 Long 경계값은 정상 생성된다" {
        val room = RoomId("${Long.MAX_VALUE}_${Long.MAX_VALUE}")
        room.participants // 예외 없이 파싱되어야 한다
    }
})
