package io.github.yunhobb.chat.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat/users/{storeSeq}")
class ChatUserController {

    @GetMapping("/unread-message-cnt")
    fun unreadMessageCount(@PathVariable storeSeq: Long): Nothing = TODO("기존 응답 스키마 확보 후 구현")

    @GetMapping("/region")
    fun region(@PathVariable storeSeq: Long): Nothing = TODO("기존 응답 스키마 확보 후 구현")

    @PostMapping("/block-user/{blockStoreSeq}")
    fun blockUser(
        @PathVariable storeSeq: Long,
        @PathVariable blockStoreSeq: Long,
    ): Nothing = TODO("기존 요청/응답 스키마 확보 후 구현")
}
