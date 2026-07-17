package io.github.yunhobb.chat.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
class ChatResourceController {

    @GetMapping("/products/{productSeq}/chat-count")
    fun productChatCount(@PathVariable productSeq: Long): Nothing = TODO("기존 응답 스키마 확보 후 구현")

    @GetMapping("/emoticons")
    fun emoticons(): Nothing = TODO("기존 응답 스키마 확보 후 구현")

    @GetMapping("/quick-buttons")
    fun quickButtons(): Nothing = TODO("기존 응답 스키마 확보 후 구현")
}
