package com.team.jaksimweek.ui.chat

import com.google.firebase.database.FirebaseDatabase

object ChatUtil {
    // chatRooms 경로를 한 곳에서 관리하여 실수를 방지합니다. (대소문자 일관성 유지)
    private val chatRoomsRef = FirebaseDatabase.getInstance().reference.child("chatRooms")
    private val userChatsRef = FirebaseDatabase.getInstance().reference.child("user-chats")

    /**
     * 1:1 채팅방을 생성하거나 기존 채팅방 ID를 가져옵니다.
     * 데이터 구조를 ChatRoom 클래스에 맞게 수정했습니다.
     */
    fun createOrGetOneToOneChatRoom(myUid: String, otherUid: String, callback: (String) -> Unit) {
        val chatRoomId = if (myUid < otherUid) "$myUid-$otherUid" else "$otherUid-$myUid"
        val roomRef = chatRoomsRef.child(chatRoomId)

        roomRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // 방이 이미 존재하면 ID만 반환
                callback(chatRoomId)
            } else {
                // 방이 없으면 새로운 ChatRoom 객체를 생성하여 저장
                val participants = mapOf(myUid to true, otherUid to true)
                val newRoom = ChatRoom(
                    roomId = chatRoomId,
                    participants = participants,
                    type = "1on1"
                    // 1:1 채팅이므로 roomName은 null, lastMessage 등은 기본값 사용
                )

                roomRef.setValue(newRoom).addOnSuccessListener {
                    // 유저별 채팅 목록에도 추가
                    userChatsRef.child(myUid).child(chatRoomId).setValue(true)
                    userChatsRef.child(otherUid).child(chatRoomId).setValue(true)
                    callback(chatRoomId)
                }.addOnFailureListener {
                    // TODO: 실패 처리 (예: Toast 메시지 표시)
                }
            }
        }
    }

    /**
     * 그룹 채팅방을 생성합니다.
     * 데이터 구조를 ChatRoom 클래스에 맞게 수정했습니다.
     */
    fun createGroupChatRoom(
        questId: String,
        participantUids: List<String>,
        questTitle: String,
        callback: (String) -> Unit
    ) {
        val chatRoomId = "group_$questId"
        val roomRef = chatRoomsRef.child(chatRoomId)

        roomRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // 이미 방이 존재하면 ID만 반환
                callback(chatRoomId)
            } else {
                // 새로운 그룹 채팅방 객체를 생성
                val newRoom = ChatRoom(
                    roomId = chatRoomId,
                    roomName = "$questTitle 챌린지 그룹방",
                    participants = participantUids.associateWith { true },
                    type = "group"
                )

                // 생성된 객체를 Firebase에 한번에 저장
                roomRef.setValue(newRoom).addOnSuccessListener {
                    // 각 참여자의 유저별 채팅 목록에도 추가
                    participantUids.forEach { uid ->
                        userChatsRef.child(uid).child(chatRoomId).setValue(true)
                    }
                    callback(chatRoomId)
                }.addOnFailureListener {
                    // TODO: 실패 처리
                }
            }
        }
    }
}