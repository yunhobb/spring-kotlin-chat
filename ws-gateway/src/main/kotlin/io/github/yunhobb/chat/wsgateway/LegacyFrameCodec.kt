package io.github.yunhobb.chat.wsgateway

import io.github.yunhobb.chat.core.domain.Message

/**
 * 기존 앱 WS 프레임 ↔ 도메인 변환.
 *
 * TODO(선행 조사 §6-1): 구 소켓 서버/앱 코드에서 프레임 규격 역설계 후 구현.
 *  웹 번들에서 TALK / ack / sync 키워드까지만 확인된 상태다. 하트비트(ping) 규격과
 *  ALB idle timeout 정합(§6-4)도 여기서 함께 확정한다.
 */
object LegacyFrameCodec {
    fun decode(raw: String): Nothing = TODO("기존 프레임 규격 역설계 후 구현")

    fun encode(message: Message): Nothing = TODO("기존 프레임 규격 역설계 후 구현")
}
