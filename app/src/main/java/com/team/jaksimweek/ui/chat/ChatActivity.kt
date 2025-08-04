package com.team.jaksimweek.ui.chat

import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.team.jaksimweek.R // 프로젝트 R 파일 경로에 맞게 수정

class ChatActivity : AppCompatActivity() {

    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: ImageButton
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var database: DatabaseReference
    private lateinit var roomId: String
    private val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val myNickname = "테스트 닉네임" // 실제 앱에서는 사용자 정보에서 가져와야 함

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        roomId = intent.getStringExtra("roomId") ?: return
        val roomType = intent.getStringExtra("roomType") ?: "group"
        val roomName = intent.getStringExtra("roomName") ?: "채팅방"

        supportActionBar?.title = roomName

        chatRecyclerView = findViewById(R.id.chatRecyclerView)
        messageEditText = findViewById(R.id.messageEditText)
        sendButton = findViewById(R.id.sendButton)

        chatRecyclerView.layoutManager = LinearLayoutManager(this)
        chatAdapter = ChatAdapter(this, myUid) // ChatAdapter에 현재 사용자 UID 전달
        chatRecyclerView.adapter = chatAdapter

        database = FirebaseDatabase.getInstance().reference

        loadMessages(roomId)

        sendButton.setOnClickListener {
            val message = messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                messageEditText.text.clear()
            }
        }
    }

    private fun loadMessages(roomId: String) {
        val messagesRef = database.child("messages").child(roomId)

        messagesRef.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(ChatMessage::class.java)
                message?.let {
                    chatAdapter.addMessage(it)
                    chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendMessage(text: String) {
        val messagesRef = database.child("messages").child(roomId).push()
        val message = ChatMessage(myUid, myNickname, text, null, System.currentTimeMillis(), "text")

        // 2. Firebase에 보낼 데이터 Map 생성
        val messageToSend = mutableMapOf<String, Any>(
            "senderUid" to message.senderUid,
            "senderNickname" to message.senderNickname,
            "text" to message.text!!,
            "messageType" to message.messageType,
            "timestamp" to ServerValue.TIMESTAMP // Firebase가 서버에서 시간을 기록하도록 설정
        )

        messagesRef.setValue(messageToSend) { error, _ ->
            if (error == null) {
                // 채팅방 마지막 메시지 업데이트
                val roomRef =
                    database.child("chatRooms").child(roomId) // <- 경로명 일관성 문제도 확인하세요 (아래 3번 항목)
                val lastMessageUpdate = mapOf(
                    "lastMessage" to text,
                    "lastMessageTimestamp" to ServerValue.TIMESTAMP
                )
                roomRef.updateChildren(lastMessageUpdate)
            } else {
                // 메시지 전송 실패 처리
            }
        }
    }
}