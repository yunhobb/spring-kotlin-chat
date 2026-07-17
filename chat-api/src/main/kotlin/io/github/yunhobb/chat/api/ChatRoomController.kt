package io.github.yunhobb.chat.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 기존 계약 유지 대상: 경로·응답 스키마 모두 현행 edge-live와 동일해야 한다 (DESIGN.md §2).
 * TODO(선행 조사 §6): 응답 DTO는 기존 API 응답 캡처로 확정한다. 그 전까지 전부 스텁.
 */
@RestController
@RequestMapping("/api/chat")
class ChatRoomController {

    @GetMapping("/rooms")
    fun rooms(): Nothing = TODO("기존 응답 스키마 확보 후 구현")

    @GetMapping("/rooms/{roomId}")
    fun room(@PathVariable roomId: String): Nothing = TODO("기존 응답 스키마 확보 후 구현")

    @GetMapping("/rooms/{roomId}/messages")
    fun messages(@PathVariable roomId: String): Nothing = TODO("기존 응답 스키마·페이징 파라미터 확보 후 구현")
}
