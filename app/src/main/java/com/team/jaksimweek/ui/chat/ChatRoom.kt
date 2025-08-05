package com.team.jaksimweek.ui.chat

data class ChatRoom(
    var roomId: String = "",
    var roomName: String? = null, // 그룹 채팅방 이름
    var lastMessage: String = "",
    var lastMessageTimestamp: Long = 0L,
    var participants: Map<String, Boolean> = emptyMap(), // 참여자 uid 목록
    var type: String = "" // "group" 또는 "1on1"
)