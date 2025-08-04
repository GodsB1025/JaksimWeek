package com.team.jaksimweek.ui.profile

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.data.model.User
import com.team.jaksimweek.databinding.ActivityProfileBinding

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var selectedImageUri: Uri? = null

    // 갤러리 런처 초기화
    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            binding.ivProfile.setImageURI(uri) // 선택한 이미지로 뷰 업데이트
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()

        // 프로필 이미지 클릭 시 갤러리 열기
        binding.ivProfile.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        // 저장 버튼 클릭 리스너
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                if (user != null) {
                    binding.tvEmail.text = user.email
                    binding.etNickname.setText(user.nickname)
                    binding.etBio.setText(user.bio)

                    // Glide를 사용해 프로필 이미지 로드
                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .circleCrop()
                        .into(binding.ivProfile)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 정보 로딩 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        val nickname = binding.etNickname.text.toString()
        val bio = binding.etBio.text.toString()

        // 1. 이미지가 변경되었는지 확인
        if (selectedImageUri != null) {
            // Firebase Storage에 이미지 업로드
            val imageRef = storage.reference.child("profile_images/$uid.jpg")
            imageRef.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl // 업로드 성공 시 다운로드 URL 가져오기
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        // 2. 이미지 URL 포함 모든 정보 업데이트
                        updateUserInfo(uid, nickname, bio, downloadUrl)
                    } else {
                        Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            // 3. 텍스트 정보만 업데이트
            updateUserInfo(uid, nickname, bio, null)
        }
    }

    private fun updateUserInfo(uid: String, nickname: String, bio: String, imageUrl: String?) {
        val updates = mutableMapOf<String, Any>(
            "nickname" to nickname,
            "bio" to bio
        )
        if (imageUrl != null) {
            updates["profileImageUrl"] = imageUrl
        }

        firestore.collection("users").document(uid).update(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "프로필이 업데이트되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 프로필 화면 종료
            }
            .addOnFailureListener {
                Toast.makeText(this, "업데이트 실패", Toast.LENGTH_SHORT).show()
            }
    }
}