package com.team.jaksimweek.ui.chat

data class ChatMessage(
    var senderUid: String = "",
    var senderNickname: String = "",
    var text: String? = null, // 텍스트 메시지
    var imageUrl: String? = null, // 사진 메시지
    var timestamp: Long = 0L,
    var messageType: String = "text" // "text" 또는 "image"
)