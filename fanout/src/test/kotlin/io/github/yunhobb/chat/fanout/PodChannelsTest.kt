package io.github.yunhobb.chat.fanout

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

/**
 * 파드 채널 네이밍 단일 소스(docs/realtime-and-session-design.md §4).
 * 구독측(PodIdentity)과 발행측(RedisFanoutPublisher)이 반드시 같은 규칙을 써야 하므로 한 곳에서 정의한다.
 */
class PodChannelsTest : StringSpec({

    "파드 채널은 chat:pod:{podId} 형식이다" {
        PodChannels.forPod("pod-abc") shouldBe "chat:pod:pod-abc"
    }

    "podId가 그대로 접미어로 붙는다" {
        PodChannels.forPod("local") shouldBe "chat:pod:local"
    }
})
