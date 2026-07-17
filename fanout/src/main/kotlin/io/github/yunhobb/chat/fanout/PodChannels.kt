package io.github.yunhobb.chat.fanout

/**
 * 파드 간 팬아웃 채널 네이밍의 단일 소스(docs/realtime-and-session-design.md §4).
 * 구독측(PodIdentity)과 발행측(RedisFanoutPublisher)이 같은 규칙을 공유하도록 여기서만 정의한다.
 */
object PodChannels {
    fun forPod(podId: String): String = "chat:pod:$podId"
}
