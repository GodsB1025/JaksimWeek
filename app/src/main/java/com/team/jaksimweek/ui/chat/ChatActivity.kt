package com.team.jaksimweek.ui.chat

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.team.jaksimweek.R // 프로젝트 R 파일 경로에 맞게 수정
import com.team.jaksimweek.databinding.ActivityChallengeDetailBinding
import com.team.jaksimweek.databinding.ActivityChatBinding
import com.google.android.material.appbar.MaterialToolbar

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatRecyclerView: RecyclerView
    private lateinit var messageEditText: EditText
    private lateinit var sendButton: Button
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var database: DatabaseReference
    private lateinit var roomId: String
    private val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    private val myNickname = "테스트 닉네임" // 실제 앱에서는 사용자 정보에서 가져와야 함

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 2) 뒤로가기(←) 클릭 리스너
        toolbar.setNavigationOnClickListener { finish() }

        // 3) 동적 타이틀: 상대방 닉네임 + "님"
        val partnerName = intent.getStringExtra("roomName") ?: "채팅방"
        toolbar.title = "$partnerName 님"

        roomId = intent.getStringExtra("roomId")?.also {
            Log.d("ChatDebug","roomId=$it")
        } ?: run {
            Log.e("ChatDebug","roomId is null!")
            finish()      // 액티비티 종료
            return
        }

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
        // 1. 메시지를 저장할 새로운 경로 생성 (push)
        val messagesRef = database.child("messages").child(roomId).push()

        // 2. Firebase에 보낼 데이터 Map 생성
        val messageToSend = mapOf(
            "senderUid" to myUid,
            "senderNickname" to myNickname,
            "text" to text,
            "messageType" to "text",
            "timestamp" to ServerValue.TIMESTAMP // Firebase 서버 시간 사용
        )

        // 3. 메시지 전송
        messagesRef.setValue(messageToSend).addOnSuccessListener {
            Log.d("ChatDebug", "✅ 메시지 전송 성공! Text: $text")
            // 메시지 전송 성공 시, 채팅방의 마지막 메시지 정보 업데이트
            val roomRef = database.child("chatRooms").child(roomId)
            val lastMessageUpdate = mapOf(
                "lastMessage" to text,
                "lastMessageTimestamp" to ServerValue.TIMESTAMP
            )
            roomRef.updateChildren(lastMessageUpdate)
        }.addOnFailureListener { exception ->
            Log.e("ChatDebug", "❌ 메시지 전송 실패!", exception)
            // 메시지 전송 실패 시 에러 처리
            // 예: Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }
}