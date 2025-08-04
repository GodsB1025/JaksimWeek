package com.team.jaksimweek.ui.chat

import com.google.firebase.database.FirebaseDatabase

object ChatUtil {
    private val database = FirebaseDatabase.getInstance().reference

    fun createOrGetOneToOneChatRoom(myUid: String, otherUid: String, callback: (String) -> Unit) {
        val chatRoomId = if (myUid < otherUid) "$myUid-$otherUid" else "$otherUid-$myUid"
        val chatRoomRef = database.child("chatRooms").child(chatRoomId)

        val updates = mapOf(
            "members/$myUid" to true,
            "members/$otherUid" to true,
            "info/isGroupChat" to false
        )

        chatRoomRef.updateChildren(updates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                database.child("user-chats").child(myUid).child(chatRoomId).setValue(true)
                database.child("user-chats").child(otherUid).child(chatRoomId).setValue(true)
                callback(chatRoomId)
            } else {
                // TODO: 실패 처리
            }
        }
    }

    fun createGroupChatRoom(
        questId: String,
        participantUids: List<String>,
        questTitle: String,
        callback: (String) -> Unit
    ) {
        val chatRoomId = "group_$questId"
        val chatRoomRef = database.child("chatRooms").child(chatRoomId)

        chatRoomRef.child("info").get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                callback(chatRoomId) // 이미 존재
            } else {
                val roomName = "$questTitle 챌린지 그룹방"
                val membersMap = participantUids.associateWith { true }

                chatRoomRef.child("info")
                    .setValue(mapOf("isGroupChat" to true, "roomName" to roomName))
                chatRoomRef.child("members").updateChildren(membersMap)

                participantUids.forEach { uid ->
                    database.child("user-chats").child(uid).child(chatRoomId).setValue(true)
                }
                callback(chatRoomId)
            }
        }
    }
}