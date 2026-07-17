package io.github.yunhobb.chat.wsgateway

import io.github.yunhobb.chat.core.usecase.SendMessageService
import org.springframework.stereotype.Component
import org.springframework.web.socket.CloseStatus
import org.springframework.web.socket.TextMessage
import org.springframework.web.socket.WebSocketSession
import org.springframework.web.socket.handler.TextWebSocketHandler

/**
 * 기존 앱의 WS 프레임 프로토콜을 그대로 구현하는 핸들러 (계약 유지 원칙).
 * 프레임 규격 역설계(§6-1)가 끝나기 전까지 수신 처리 로직은 스텁이다.
 */
@Component
class LegacyChatWebSocketHandler(
    private val localSessions: LocalSessionMap,
    private val sendMessage: SendMessageService,
) : TextWebSocketHandler() {

    override fun afterConnectionEstablished(session: WebSocketSession) {
        // TODO(선행 조사 §6-1): 핸드셰이크에서 JWT 검증 → storeSeq 추출 →
        //  localSessions.add(storeSeq, session) + SessionRegistry.register(storeSeq, 이 파드)
    }

    override fun handleTextMessage(session: WebSocketSession, message: TextMessage) {
        // TODO(선행 조사 §6-1): LegacyFrameCodec.decode → 프레임 타입 분기(TALK/ack/sync 확인됨) →
        //  sendMessage.send(...) 등 chat-core 유스케이스 호출
    }

    override fun afterConnectionClosed(session: WebSocketSession, status: CloseStatus) {
        // TODO: localSessions.remove + SessionRegistry.unregister.
        //  파드 강제 종료 대비 레지스트리 TTL이 최종 안전망이다 (docs/architecture.md §3).
    }
}
