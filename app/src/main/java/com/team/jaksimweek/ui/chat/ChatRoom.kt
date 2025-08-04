package com.team.jaksimweek.ui.chat

data class ChatRoom(
    val roomId: String = "",
    val roomName: String? = null, // 그룹 채팅방 이름
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val participants: Map<String, Boolean> = emptyMap(), // 참여자 uid 목록
    val type: String = "" // "group" 또는 "1on1"
)