package io.github.yunhobb.chat.fanout

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * EKS에서는 Downward API로 POD_NAME 환경변수를 주입한다.
 * 로컬 개발 시에는 기본값 "local"을 쓴다.
 */
@Component
class PodIdentity(
    @Value("\${POD_NAME:local}") val podId: String,
) {
    /** 이 파드가 구독하는 팬아웃 채널. */
    val channel: String = PodChannels.forPod(podId)
}
