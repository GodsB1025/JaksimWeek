package com.team.jaksimweek.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.team.jaksimweek.data.model.ChatMessage
import com.team.jaksimweek.data.model.ChatRoom
import com.team.jaksimweek.data.model.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow

class ChatRepository {
    private val auth = FirebaseAuth.getInstance()
    private val database = FirebaseDatabase.getInstance().reference
    private val firestore = FirebaseFirestore.getInstance()

    private val _chatRooms = MutableStateFlow<List<ChatRoom>>(emptyList())
    val chatRooms: StateFlow<List<ChatRoom>> = _chatRooms

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount

    private val _newMessageEvent = MutableSharedFlow<Pair<ChatRoom, ChatMessage>>()
    val newMessageEvent = _newMessageEvent.asSharedFlow()

    private val chatRoomList = mutableListOf<ChatRoom>()
    private val chatRoomListeners = mutableMapOf<String, ValueEventListener>()
    private val messageListeners = mutableMapOf<String, ChildEventListener>()
    private var userChatsListener: ChildEventListener? = null
    private lateinit var userChatsRef: DatabaseReference

    init {
        auth.currentUser?.uid?.let {
            userChatsRef = database.child("user-chats").child(it)
            attachUserChatsListener(it)
        }
    }

    private fun attachUserChatsListener(myUid: String) {
        cleanup()

        userChatsListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val roomId = snapshot.key ?: return
                val unreadCount = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0
                listenForChatRoomUpdates(roomId, myUid, unreadCount)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val roomId = snapshot.key ?: return
                val unreadCount = snapshot.child("unreadCount").getValue(Int::class.java) ?: 0
                val roomIndex = chatRoomList.indexOfFirst { it.roomId == roomId }
                if (roomIndex != -1) {
                    val updatedRoom = chatRoomList[roomIndex].copy(unreadCount = unreadCount)
                    updateChatRoomList(roomId, updatedRoom)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val roomId = snapshot.key ?: return
                removeChatRoomListener(roomId)
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        }.also {
            userChatsRef.addChildEventListener(it)
        }
    }

    private fun listenForChatRoomUpdates(roomId: String, myUid: String, unreadCount: Int) {
        val chatRoomRef = database.child("chatRooms").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatRoom = snapshot.getValue(ChatRoom::class.java)
                chatRoom?.let {
                    it.unreadCount = unreadCount
                    if (it.type == "1on1") {
                        val partnerUid = it.participants.keys.firstOrNull { uid -> uid != myUid }
                        if (partnerUid != null) {
                            it.partnerUid = partnerUid
                            firestore.collection("users").document(partnerUid).get()
                                .addOnSuccessListener { userDocument ->
                                    val partnerUser = userDocument.toObject(User::class.java)
                                    it.partnerNickname = partnerUser?.nickname
                                    it.partnerProfileImageUrl = partnerUser?.profileImageUrl
                                    updateChatRoomList(roomId, it)
                                }
                        }
                    } else {
                        updateChatRoomList(roomId, it)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        chatRoomRef.addValueEventListener(listener)
        chatRoomListeners[roomId] = listener
        listenForNewMessages(roomId, myUid)
    }

    private fun listenForNewMessages(roomId: String, myUid: String) {
        val messagesRef = database.child("messages").child(roomId)
        var lastKnownTimestamp = 0L

        messagesRef.orderByChild("timestamp").limitToLast(1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.firstOrNull()?.let {
                    lastKnownTimestamp = it.child("timestamp").getValue(Long::class.java) ?: 0L
                }

                val messageListener = object : ChildEventListener {
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        val message = snapshot.getValue(ChatMessage::class.java)
                        if (message != null && message.timestamp > lastKnownTimestamp) {
                            val room = chatRoomList.find { it.roomId == roomId }
                            if (room != null && message.senderUid != myUid) {
                                _newMessageEvent.tryEmit(Pair(room, message))
                            }
                            lastKnownTimestamp = message.timestamp
                        }
                    }
                    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onChildRemoved(snapshot: DataSnapshot) {}
                    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
                    override fun onCancelled(error: DatabaseError) {}
                }
                messagesRef.orderByChild("timestamp").startAt(lastKnownTimestamp.toDouble() + 1).addChildEventListener(messageListener)
                messageListeners[roomId] = messageListener
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun updateChatRoomList(roomId: String, chatRoom: ChatRoom) {
        val existingIndex = chatRoomList.indexOfFirst { it.roomId == roomId }
        if (existingIndex != -1) {
            chatRoomList[existingIndex] = chatRoom
        } else {
            chatRoomList.add(chatRoom)
        }
        chatRoomList.sortByDescending { it.lastMessageTimestamp }
        _chatRooms.value = chatRoomList.toList()
        updateTotalUnreadCount()
    }

    private fun updateTotalUnreadCount() {
        val total = chatRoomList.sumOf { it.unreadCount }
        _totalUnreadCount.value = total
    }


    private fun removeChatRoomListener(roomId: String) {
        chatRoomListeners[roomId]?.let {
            database.child("chatRooms").child(roomId).removeEventListener(it)
            chatRoomListeners.remove(roomId)
        }
        messageListeners[roomId]?.let {
            database.child("messages").child(roomId).removeEventListener(it)
            messageListeners.remove(roomId)
        }
        val roomIndex = chatRoomList.indexOfFirst { it.roomId == roomId }
        if (roomIndex != -1) {
            chatRoomList.removeAt(roomIndex)
            _chatRooms.value = chatRoomList.toList()
            updateTotalUnreadCount()
        }
    }

    fun cleanup() {
        userChatsListener?.let { userChatsRef.removeEventListener(it) }
        chatRoomListeners.forEach { (roomId, listener) ->
            database.child("chatRooms").child(roomId).removeEventListener(listener)
        }
        messageListeners.forEach { (roomId, listener) ->
            val query = database.child("messages").child(roomId).orderByChild("timestamp")
            query.removeEventListener(listener)
        }
        chatRoomListeners.clear()
        messageListeners.clear()
    }
}