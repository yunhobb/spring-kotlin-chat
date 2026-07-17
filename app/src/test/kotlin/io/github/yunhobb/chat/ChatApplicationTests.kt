package io.github.yunhobb.chat

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

/**
 * 전 모듈 빈 조립이 깨지지 않는지 확인하는 스모크 테스트.
 * 외부 인프라(Redis/AWS)에는 연결하지 않는다 — 클라이언트 빈은 전부 lazy 연결.
 */
@SpringBootTest
class ChatApplicationTests {

    @Test
    fun contextLoads() {
    }
}
