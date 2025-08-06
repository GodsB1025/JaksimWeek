package com.team.jaksimweek.ui.chat

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.R
import com.team.jaksimweek.adapter.ChatAdapter
import com.team.jaksimweek.adapter.ParticipantAdapter
import com.team.jaksimweek.data.model.ChatMessage
import com.team.jaksimweek.data.model.ChatRoom
import com.team.jaksimweek.data.model.User
import com.team.jaksimweek.databinding.ActivityChatBinding
import com.team.jaksimweek.databinding.DialogParticipantListBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private lateinit var chatAdapter: ChatAdapter
    private lateinit var database: DatabaseReference
    private lateinit var roomId: String

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var myUserInfo: User? = null
    private var currentChatRoom: ChatRoom? = null
    private var roomType: String? = null
    private var photoUri: Uri? = null

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                dispatchTakePictureIntent()
            } else {
                Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show()
            }
        }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { isSuccess ->
        if (isSuccess) {
            photoUri?.let {
                uploadImage(it)
            }
        }
    }

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

        database.child("chatRooms").child(roomId).get().addOnSuccessListener {
            currentChatRoom = it.getValue(ChatRoom::class.java)
            invalidateOptionsMenu()
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
            showImageSelectionDialog()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (currentChatRoom?.type == "group") {
            menuInflater.inflate(R.menu.chat_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_view_participants -> {
                showParticipantList()
                true
            }
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showParticipantList() {
        val participantUids = currentChatRoom?.participants?.keys?.toList()
        if (participantUids.isNullOrEmpty()) {
            Toast.makeText(this, "참여자 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userList = mutableListOf<User>()
        val dialogBinding = DialogParticipantListBinding.inflate(layoutInflater)
        val dialog = AlertDialog.Builder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.rvParticipants.layoutManager = LinearLayoutManager(this)

        firestore.collection("users").whereIn("uid", participantUids).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    val user = document.toObject(User::class.java)
                    userList.add(user)
                }
                dialogBinding.rvParticipants.adapter = ParticipantAdapter(userList)
                dialog.show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "참여자 목록을 불러오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
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

    private fun showImageSelectionDialog() {
        val options = arrayOf("카메라로 촬영하기", "갤러리에서 선택하기")
        AlertDialog.Builder(this)
            .setTitle("이미지 전송")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> galleryLauncher.launch("image/*")
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                dispatchTakePictureIntent()
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    private fun dispatchTakePictureIntent() {
        val photoFile: File? = try {
            createImageFile()
        } catch (ex: IOException) {
            Toast.makeText(this, "이미지 파일을 생성하는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
            null
        }
        photoFile?.also {
            val photoURI: Uri = FileProvider.getUriForFile(
                this,
                "${applicationContext.packageName}.provider",
                it
            )
            photoUri = photoURI
            takePictureLauncher.launch(photoURI)
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date())
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_",
            ".jpg",
            storageDir
        )
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