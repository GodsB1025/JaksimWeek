package com.team.jaksimweek.data.model

data class ChatMessage(
    var senderUid: String = "",
    var senderNickname: String = "",
    var text: String? = null,
    var imageUrl: String? = null,
    var timestamp: Long = 0L,
    var messageType: String = "text",
    var senderProfileImageUrl: String? = null
)