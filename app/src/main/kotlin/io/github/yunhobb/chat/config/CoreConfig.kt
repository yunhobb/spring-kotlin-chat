package io.github.yunhobb.chat.config

import io.github.yunhobb.chat.core.port.FanoutPublisher
import io.github.yunhobb.chat.core.port.MessageRepository
import io.github.yunhobb.chat.core.port.OfflinePushQueue
import io.github.yunhobb.chat.core.port.SessionRegistry
import io.github.yunhobb.chat.core.usecase.SendMessageService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * chat-core는 Spring 무의존이므로 유스케이스 빈 조립은 여기서만 한다 (docs/architecture.md §4).
 */
@Configuration
class CoreConfig {

    @Bean
    fun sendMessageService(
        messages: MessageRepository,
        sessions: SessionRegistry,
        fanout: FanoutPublisher,
        offlinePush: OfflinePushQueue,
    ): SendMessageService = SendMessageService(messages, sessions, fanout, offlinePush)
}
