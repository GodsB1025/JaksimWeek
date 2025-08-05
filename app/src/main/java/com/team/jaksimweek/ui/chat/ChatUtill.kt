package com.team.jaksimweek.ui.chat

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.team.jaksimweek.data.model.ChatRoom

object ChatUtil {
    private val chatRoomsRef = FirebaseDatabase.getInstance().reference.child("chatRooms")
    private val userChatsRef = FirebaseDatabase.getInstance().reference.child("user-chats")

    fun createOrGetOneToOneChatRoom(myUid: String, otherUid: String, callback: (String) -> Unit) {
        Log.d("ChatDebug", "🔷 ChatUtil.start for myUid=$myUid, otherUid=$otherUid")
        val chatRoomId = if (myUid < otherUid) "$myUid-$otherUid" else "$otherUid-$myUid"
        val roomRef = chatRoomsRef.child(chatRoomId)

        roomRef.get().addOnSuccessListener { snapshot ->
            Log.d("ChatDebug", "🔷 ChatUtil.get() succeeded, exists=${snapshot.exists()}")
            if (snapshot.exists()) {
                callback(chatRoomId)
            } else {
                val participants = mapOf(myUid to true, otherUid to true)
                val newRoom = ChatRoom(
                    roomId = chatRoomId,
                    participants = participants,
                    type = "1on1"
                )

                roomRef.setValue(newRoom).addOnSuccessListener {
                    Log.d("ChatDebug", "🔷 ChatUtil.setValue() succeeded, roomId=$chatRoomId")
                    userChatsRef.child(myUid).child(chatRoomId).setValue(true)
                    userChatsRef.child(otherUid).child(chatRoomId).setValue(true)
                    callback(chatRoomId)
                }.addOnFailureListener { e ->
                    Log.e("ChatDebug", "🔶 ChatUtil.setValue() failed", e)
                }
            }
        }.addOnFailureListener { e ->
            Log.e("ChatDebug", "🔶 ChatUtil.get() failed", e)
        }
    }

    fun createGroupChatRoom(
        challengeId: String,
        challengeTitle: String,
        participantUids: List<String>,
        callback: (String) -> Unit
    ) {
        val chatRoomId = "group_$challengeId"
        val roomRef = chatRoomsRef.child(chatRoomId)

        roomRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                callback(chatRoomId)
            } else {
                val newRoom = ChatRoom(
                    roomId = chatRoomId,
                    roomName = "$challengeTitle 챌린지 그룹방",
                    participants = participantUids.associateWith { true },
                    type = "group"
                )

                roomRef.setValue(newRoom).addOnSuccessListener {
                    participantUids.forEach { uid ->
                        userChatsRef.child(uid).child(chatRoomId).setValue(true)
                    }
                    callback(chatRoomId)
                }.addOnFailureListener {
                    Log.e("ChatUtil", "그룹 채팅방 생성 실패", it)
                }
            }
        }.addOnFailureListener {
            Log.e("ChatUtil", "그룹 채팅방 조회 실패", it)
        }
    }
}