package io.github.yunhobb.chat.wsgateway

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

/**
 * 이 파드에 붙어 있는 소켓만 담는 인메모리 맵.
 * 전역 위치 정보는 SessionRegistry(Redis)가 담당하고, 여기는 실제 push 대상 소켓 조회용이다.
 *
 * 동시성(docs/realtime-and-session-design.md §3): 집합 변경과 빈 엔트리 정리를
 * ConcurrentHashMap.compute의 키 단위 원자 블록 안에서 수행해 잃어버린 갱신(lost-update)을 막는다.
 * 순진하게 isEmpty() 확인 후 remove(key, set)을 하면, 그 사이 다른 스레드의 add가 유실될 수 있다.
 */
@Component
class LocalSessionMap {
    private val sessions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    fun add(storeSeq: Long, session: WebSocketSession) {
        sessions.compute(storeSeq) { _, set ->
            (set ?: ConcurrentHashMap.newKeySet()).also { it.add(session) }
        }
    }

    fun remove(storeSeq: Long, session: WebSocketSession) {
        sessions.computeIfPresent(storeSeq) { _, set ->
            set.remove(session)
            if (set.isEmpty()) null else set // null 반환 = 엔트리 원자적 제거
        }
    }

    /** 브로드캐스트 중 외부 변경·내부 노출을 막기 위해 불변 스냅샷을 반환한다. */
    fun sessionsOf(storeSeq: Long): Set<WebSocketSession> = sessions[storeSeq]?.toSet() ?: emptySet()

    /** 원자 정리 덕분에 키가 존재하면 항상 세션이 1개 이상이다 → 온라인 판정에 사용 가능. */
    fun isOnline(storeSeq: Long): Boolean = sessions.containsKey(storeSeq)

    fun connectionCount(): Int = sessions.values.sumOf { it.size }
}
