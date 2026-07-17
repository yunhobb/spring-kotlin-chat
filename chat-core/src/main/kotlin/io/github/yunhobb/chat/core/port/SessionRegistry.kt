package io.github.yunhobb.chat.core.port

/** 유저의 WebSocket 연결이 살아있는 파드 위치. */
data class SessionLocation(
    val podId: String,
    val deviceId: String? = null,
)

/**
 * 구현: fanout(Redis). user → pod 매핑으로 타겟 라우팅과 오프라인 판정을 담당한다.
 * 파드 비정상 종료로 남는 유령 엔트리는 TTL로 정리한다 (DESIGN.md §3).
 */
interface SessionRegistry {
    fun register(storeSeq: Long, location: SessionLocation)

    fun unregister(storeSeq: Long, location: SessionLocation)

    /** 비어 있으면 오프라인 → 푸시 알림 경로로 전환. */
    fun locate(storeSeq: Long): List<SessionLocation>
}
