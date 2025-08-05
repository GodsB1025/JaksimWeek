package com.team.jaksimweek.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.team.jaksimweek.adapter.ChatRoomAdapter
import com.team.jaksimweek.databinding.FragmentChatBinding
import com.team.jaksimweek.ui.chat.ChatActivity
import com.team.jaksimweek.data.model.ChatRoom
import com.team.jaksimweek.ui.chat.UserSearchActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.team.jaksimweek.data.model.User

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val database by lazy { FirebaseDatabase.getInstance().reference }
    private val chatRooms = mutableListOf<ChatRoom>()
    private val chatRoomListeners = mutableMapOf<String, ValueEventListener>()
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadUserChatRooms()

        binding.btnStartChatFlow.setOnClickListener {
            startActivity(Intent(requireContext(), UserSearchActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        chatRoomAdapter = ChatRoomAdapter { chatRoom ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("roomId", chatRoom.roomId)
                putExtra("roomName", chatRoom.roomName)
            }
            startActivity(intent)
        }
        binding.rvChatRooms.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatRoomAdapter
        }
    }

    private fun loadUserChatRooms() {
        val myUid = auth.currentUser?.uid ?: return
        val userChatsRef = database.child("user-chats").child(myUid)

        userChatsRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val roomId = snapshot.key ?: return
                listenForChatRoomUpdates(roomId)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}

            override fun onChildRemoved(snapshot: DataSnapshot) {
                val roomId = snapshot.key ?: return
                removeChatRoomListener(roomId)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatFragment", "Failed to load user chat rooms.", error.toException())
            }
        })
    }

    private fun listenForChatRoomUpdates(roomId: String) {
        val chatRoomRef = database.child("chatRooms").child(roomId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatRoom = snapshot.getValue(ChatRoom::class.java)
                chatRoom?.let {
                    if (it.type == "1on1") {
                        val myUid = auth.currentUser?.uid
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

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChatFragment", "Failed to listen for chat room updates.", error.toException())
            }
        }
        chatRoomRef.addValueEventListener(listener)
        chatRoomListeners[roomId] = listener
    }

    private fun updateChatRoomList(roomId: String, chatRoom: ChatRoom) {
        val existingIndex = chatRooms.indexOfFirst { it.roomId == roomId }
        if (existingIndex != -1) {
            chatRooms[existingIndex] = chatRoom
        } else {
            chatRooms.add(chatRoom)
        }
        chatRooms.sortByDescending { room -> room.lastMessageTimestamp }
        chatRoomAdapter.submitList(chatRooms.toList())
    }

    private fun removeChatRoomListener(roomId: String) {
        chatRoomListeners[roomId]?.let {
            database.child("chatRooms").child(roomId).removeEventListener(it)
            chatRoomListeners.remove(roomId)
        }
        val roomIndex = chatRooms.indexOfFirst { it.roomId == roomId }
        if (roomIndex != -1) {
            chatRooms.removeAt(roomIndex)
            chatRoomAdapter.submitList(chatRooms.toList())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatRoomListeners.forEach { (roomId, listener) ->
            database.child("chatRooms").child(roomId).removeEventListener(listener)
        }
        chatRoomListeners.clear()
        _binding = null
    }
}