package io.github.yunhobb.chat.fanout

import org.springframework.stereotype.Component

/**
 * 이 파드의 채널(chat:pod:{podId})을 구독해서 수신한 메시지를 로컬 소켓으로 밀어준다.
 *
 * TODO: 팬아웃 페이로드 포맷 확정 후 RedisMessageListenerContainer 등록.
 *  수신 → ws-gateway 쪽 push는 chat-core에 inbound port(예: MessagePushTarget)를 추가해
 *  의존 방향(fanout → core ← ws-gateway)을 유지한 채 연결한다. fanout이 ws-gateway를
 *  직접 참조하면 게이트웨이 분리(§4)가 막힌다.
 */
@Component
class PodChannelSubscriber(
    private val podIdentity: PodIdentity,
)
