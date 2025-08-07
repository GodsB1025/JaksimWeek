package com.team.jaksimweek.data.model

data class ChatRoom(
    var roomId: String = "",
    var roomName: String? = null,
    var lastMessage: String = "",
    var lastMessageTimestamp: Long = 0L,
    var participants: Map<String, Boolean> = emptyMap(),
    var type: String = "",
    var partnerUid: String? = null,
    var partnerNickname: String? = null,
    var partnerProfileImageUrl: String? = null,
    var unreadCount: Int = 0
)