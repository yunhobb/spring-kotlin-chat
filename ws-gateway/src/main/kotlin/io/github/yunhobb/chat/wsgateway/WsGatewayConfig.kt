package io.github.yunhobb.chat.wsgateway

import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
class WsGatewayConfig(
    private val handler: LegacyChatWebSocketHandler,
) : WebSocketConfigurer {

    override fun registerWebSocketHandlers(registry: WebSocketHandlerRegistry) {
        // TODO(선행 조사 §6-1): 기존 소켓 서버의 실제 경로·핸드셰이크 규격(JWT 전달 위치, 쿼리/헤더)에 맞춘다.
        //  앱 무수정 원칙이므로 경로 하나도 임의로 정할 수 없다.
        registry.addHandler(handler, "/ws")
    }
}
