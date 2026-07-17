package io.github.yunhobb.chat.integration

import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

/**
 * 타 서비스 연동의 anti-corruption layer.
 * 실사에서 확인된 연동 지점: cross-function-api의 chat-available(탈퇴 회원 채팅 불가 판정),
 * 상품 정보(상품 카드), 안심결제(구매하기 버튼).
 */
@Component
class UserServiceClient(
    builder: RestClient.Builder,
) {
    private val client: RestClient = builder.build()

    fun isChatAvailable(storeSeq: Long): Boolean {
        TODO("cross-function-api 연동 스펙 확인 후 구현")
    }
}
