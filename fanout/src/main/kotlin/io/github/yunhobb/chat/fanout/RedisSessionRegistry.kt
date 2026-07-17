package io.github.yunhobb.chat.fanout

import io.github.yunhobb.chat.core.port.SessionLocation
import io.github.yunhobb.chat.core.port.SessionRegistry
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * user → pod 매핑. 키에 TTL을 걸고 하트비트마다 갱신해서
 * 파드 비정상 종료로 남는 유령 엔트리를 정리한다 (DESIGN.md §3).
 */
@Component
class RedisSessionRegistry(
    private val redis: StringRedisTemplate,
) : SessionRegistry {

    override fun register(storeSeq: Long, location: SessionLocation) {
        TODO("키 구조·TTL 정책 확정 후 구현 (하트비트 주기 §6-4에 종속)")
    }

    override fun unregister(storeSeq: Long, location: SessionLocation) {
        TODO("키 구조·TTL 정책 확정 후 구현")
    }

    override fun locate(storeSeq: Long): List<SessionLocation> {
        TODO("키 구조·TTL 정책 확정 후 구현")
    }
}
