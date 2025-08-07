package com.team.jaksimweek.viewmodel

import androidx.lifecycle.ViewModel
import com.team.jaksimweek.data.model.ChatMessage
import com.team.jaksimweek.data.model.ChatRoom
import com.team.jaksimweek.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class MainViewModel : ViewModel() {
    private val repository = ChatRepository()

    val chatRooms: StateFlow<List<ChatRoom>> = repository.chatRooms
    val totalUnreadCount: StateFlow<Int> = repository.totalUnreadCount
    val newMessageEvent: SharedFlow<Pair<ChatRoom, ChatMessage>> = repository.newMessageEvent

    private val _currentChatRoomId = MutableStateFlow<String?>(null)
    val currentChatRoomId: StateFlow<String?> = _currentChatRoomId

    fun setCurrentChatRoomId(roomId: String?) {
        _currentChatRoomId.value = roomId
    }

    override fun onCleared() {
        super.onCleared()
        repository.cleanup()
    }
}