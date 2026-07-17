package io.github.yunhobb.chat.core.domain

/**
 * 기존 시스템의 방 식별자 형식 "{storeSeq}_{storeSeq}"를 그대로 유지한다 (계약 유지 원칙).
 *
 * TODO(선행 조사 docs/architecture.md §6-2): 두 storeSeq의 순서 규칙(정렬 canonical인지, 생성자 기준인지)은
 *  기존 DynamoDB 데이터 확인 전까지 미확정. 확정 전에는 [of]를 신규 방 생성에 사용하지 말 것.
 */
@JvmInline
value class RoomId(val value: String) {
    init {
        require(FORMAT.matches(value)) { "roomId 형식 위반: $value" }
        // fail-fast: 형식이 맞아도 storeSeq가 Long 범위를 넘으면 생성 시점에 거부한다.
        // (지연 파싱 시 NumberFormatException으로 터지는 불일치 방지)
        val (a, b) = value.split('_')
        requireNotNull(a.toLongOrNull()) { "roomId storeSeq가 Long 범위를 벗어남: $value" }
        requireNotNull(b.toLongOrNull()) { "roomId storeSeq가 Long 범위를 벗어남: $value" }
    }

    val participants: Pair<Long, Long>
        get() {
            val (a, b) = value.split('_')
            return a.toLong() to b.toLong()
        }

    fun contains(storeSeq: Long): Boolean {
        val (a, b) = participants
        return storeSeq == a || storeSeq == b
    }

    /** 1:1 방에서 [me]의 상대 storeSeq. [me]가 참여자가 아니면 예외. */
    fun otherParticipant(me: Long): Long {
        val (a, b) = participants
        return when (me) {
            a -> b
            b -> a
            else -> throw IllegalArgumentException("storeSeq $me 는 방 $value 의 참여자가 아니다")
        }
    }

    override fun toString(): String = value

    companion object {
        private val FORMAT = Regex("""\d+_\d+""")

        fun of(storeSeqA: Long, storeSeqB: Long): RoomId = RoomId("${storeSeqA}_$storeSeqB")
    }
}
