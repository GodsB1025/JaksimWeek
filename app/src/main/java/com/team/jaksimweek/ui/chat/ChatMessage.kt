package com.team.jaksimweek.ui.chat

data class ChatMessage(
    val senderUid: String = "",
    val senderNickname: String = "",
    val text: String? = null, // 텍스트 메시지
    val imageUrl: String? = null, // 사진 메시지
    val timestamp: Long = 0L,
    val messageType: String = "text" // "text" 또는 "image"
)