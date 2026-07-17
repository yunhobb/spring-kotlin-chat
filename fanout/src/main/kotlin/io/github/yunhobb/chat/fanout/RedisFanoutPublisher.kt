package io.github.yunhobb.chat.fanout

import io.github.yunhobb.chat.core.domain.Message
import io.github.yunhobb.chat.core.port.FanoutPublisher
import io.github.yunhobb.chat.core.port.SessionLocation
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.stereotype.Component

/**
 * 대상 유저가 붙은 파드의 채널("chat:pod:{podId}")로 발행한다.
 * 실시간 홉은 best-effort — 유실은 DB + 클라이언트 재조회가 복구한다 (docs/architecture.md §3).
 */
@Component
class RedisFanoutPublisher(
    private val redis: StringRedisTemplate,
) : FanoutPublisher {

    override fun publish(target: SessionLocation, message: Message) {
        // 채널 라우팅은 PodChannels로 확정됨. 페이로드 직렬화 포맷만 남았다.
        // TODO: 직렬화 포맷 확정 후 구현. 컷오버 Phase 2의 신구 브리지(§6-3)와 포맷을 맞춰야 한다.
        //  val channel = PodChannels.forPod(target.podId)
        //  redis.convertAndSend(channel, <serialized payload>)
        TODO("팬아웃 페이로드 직렬화 포맷 확정 후 구현")
    }
}
