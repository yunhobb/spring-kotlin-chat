package io.github.yunhobb.chat.wsgateway

import java.util.concurrent.ConcurrentHashMap
import org.springframework.stereotype.Component
import org.springframework.web.socket.WebSocketSession

/**
 * 이 파드에 붙어 있는 소켓만 담는 인메모리 맵.
 * 전역 위치 정보는 SessionRegistry(Redis)가 담당하고, 여기는 실제 push 대상 소켓 조회용이다.
 */
@Component
class LocalSessionMap {
    private val sessions = ConcurrentHashMap<Long, MutableSet<WebSocketSession>>()

    fun add(storeSeq: Long, session: WebSocketSession) {
        sessions.computeIfAbsent(storeSeq) { ConcurrentHashMap.newKeySet() }.add(session)
    }

    fun remove(storeSeq: Long, session: WebSocketSession) {
        sessions[storeSeq]?.let {
            it.remove(session)
            if (it.isEmpty()) sessions.remove(storeSeq, it)
        }
    }

    fun sessionsOf(storeSeq: Long): Set<WebSocketSession> = sessions[storeSeq] ?: emptySet()

    fun connectionCount(): Int = sessions.values.sumOf { it.size }
}
