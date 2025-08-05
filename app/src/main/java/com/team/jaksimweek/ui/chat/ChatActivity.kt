package com.team.jaksimweek.ui.chat

import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.data.model.ChatMessage
import com.team.jaksimweek.data.model.User
import com.team.jaksimweek.databinding.ActivityChatBinding

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var database: DatabaseReference
    private lateinit var roomId: String

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var myUserInfo: User? = null

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            uploadImage(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        database = FirebaseDatabase.getInstance().reference

        fetchMyUserInfo()

        setSupportActionBar(binding.topAppBar)
        supportActionBar?.setDisplayShowTitleEnabled(true)
        binding.topAppBar.setNavigationOnClickListener { finish() }

        val roomTitle = intent.getStringExtra("roomName") ?: "채팅방"
        supportActionBar?.title = roomTitle

        roomId = intent.getStringExtra("roomId") ?: run {
            finish()
            return
        }

        setupRecyclerView()
        loadMessages(roomId)

        binding.sendButton.setOnClickListener {
            val message = binding.messageEditText.text.toString().trim()
            if (message.isNotEmpty()) {
                sendTextMessage(message)
                binding.messageEditText.text.clear()
            }
        }

        binding.btnAddImage.setOnClickListener {
            galleryLauncher.launch("image/*")
        }
    }

    private fun setupRecyclerView() {
        val myUid = auth.currentUser?.uid ?: ""
        chatAdapter = ChatAdapter(myUid)
        binding.chatRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
            adapter = chatAdapter
        }
    }

    private fun fetchMyUserInfo() {
        val myUid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(myUid).get()
            .addOnSuccessListener { document ->
                myUserInfo = document.toObject(User::class.java)
            }
    }

    private fun loadMessages(roomId: String) {
        val messagesRef = database.child("messages").child(roomId)
        messagesRef.orderByChild("timestamp").addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(ChatMessage::class.java)
                message?.let {
                    chatAdapter.addMessage(it)
                    binding.chatRecyclerView.scrollToPosition(chatAdapter.itemCount - 1)
                }
            }
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun sendMessage(messageData: Map<String, Any?>) {
        val messagesRef = database.child("messages").child(roomId).push()
        messagesRef.setValue(messageData).addOnSuccessListener {
            val lastMessageText = if (messageData["messageType"] == "image") "사진을 보냈습니다." else messageData["text"].toString()
            val roomRef = database.child("chatRooms").child(roomId)
            val lastMessageUpdate = mapOf(
                "lastMessage" to lastMessageText,
                "lastMessageTimestamp" to ServerValue.TIMESTAMP
            )
            roomRef.updateChildren(lastMessageUpdate)
        }.addOnFailureListener {
            Toast.makeText(this, "메시지 전송에 실패했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendTextMessage(text: String) {
        if (myUserInfo == null) {
            Toast.makeText(this, "사용자 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val messageToSend = mapOf(
            "senderUid" to myUserInfo!!.uid,
            "senderNickname" to myUserInfo!!.nickname,
            "senderProfileImageUrl" to myUserInfo!!.profileImageUrl,
            "text" to text,
            "messageType" to "text",
            "timestamp" to ServerValue.TIMESTAMP
        )
        sendMessage(messageToSend)
    }

    private fun uploadImage(uri: Uri) {
        if (myUserInfo == null) {
            Toast.makeText(this, "사용자 정보를 불러오는 중입니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val fileName = "CHAT_${System.currentTimeMillis()}.png"
        val imageRef = storage.reference.child("chat_images/$roomId/$fileName")

        imageRef.putFile(uri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    val messageToSend = mapOf(
                        "senderUid" to myUserInfo!!.uid,
                        "senderNickname" to myUserInfo!!.nickname,
                        "senderProfileImageUrl" to myUserInfo!!.profileImageUrl,
                        "imageUrl" to downloadUrl.toString(),
                        "messageType" to "image",
                        "timestamp" to ServerValue.TIMESTAMP
                    )
                    sendMessage(messageToSend)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}