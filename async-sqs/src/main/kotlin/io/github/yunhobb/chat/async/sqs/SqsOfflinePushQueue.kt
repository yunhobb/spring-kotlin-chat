package io.github.yunhobb.chat.async.sqs

import io.github.yunhobb.chat.core.domain.Message
import io.github.yunhobb.chat.core.port.OfflinePushQueue
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sqs.SqsClient

/**
 * 오프라인 수신자용 푸시 알림 작업 발행 (DESIGN.md §3 비동기 경로).
 * 사기 감지 파이프라인 연동(§6-5)도 이 모듈에 어댑터로 추가된다.
 *
 * TODO: 기존 푸시 발송 큐를 재사용할지, 신규 큐 + 신규 워커로 갈지는
 *  구 시스템의 푸시 워커 구조 확인 후 결정.
 */
@Component
class SqsOfflinePushQueue(
    private val sqs: SqsClient,
) : OfflinePushQueue {

    override fun enqueue(recipientSeq: Long, message: Message) {
        TODO("푸시 큐 URL·메시지 포맷 확정 후 구현")
    }
}
