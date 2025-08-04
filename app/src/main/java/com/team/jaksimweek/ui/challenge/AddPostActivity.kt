package com.team.jaksimweek.ui.challenge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.team.jaksimweek.databinding.ActivityAddPostBinding // 생성된 ViewBinding 클래스
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.storage.FirebaseStorage
import java.util.Date

class AddPostActivity : AppCompatActivity() {
    // ViewBinding 설정
    private lateinit var binding: ActivityAddPostBinding

    // Firebase 인스턴스 (lazy 초기화)
    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    // 선택된 데이터를 담을 변수
    private var imageUri: Uri? = null
    private var location: GeoPoint? = null
    private var locationAddress: String? = null

    // FAB 애니메이션
    private val fabOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_open) }
    private val fabClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_close) }
    private var isFabOpen = false

    // 갤러리 앱 결과를 처리하는 Launcher
    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.postImageView.setImageURI(uri)
                binding.postImageView.visibility = View.VISIBLE // 이미지 뷰 보이기
            }
        }

    // 지도 앱 결과를 처리하는 Launcher
    private val mapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val lat = result.data?.getDoubleExtra("latitude", 0.0)
                val lng = result.data?.getDoubleExtra("longitude", 0.0)
                val address = result.data?.getStringExtra("address")

                if (lat != null && lng != null && address != null) {
                    location = GeoPoint(lat, lng)
                    locationAddress = address
                    binding.locationTextView.text = "📍 $address" // 선택된 주소 표시
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(binding.root)

        setupClickListeners()

    }
    // 클릭 리스너 설정 함수
    private fun setupClickListeners() {
        // 메인 FAB
        binding.fabMain.setOnClickListener {
            toggleFab()
        }

        // 사진 추가 FAB
        binding.fabAddImage.setOnClickListener {
            galleryLauncher.launch("image/*")
            toggleFab()
        }

        // 위치 선택 FAB
        binding.fabAddLocation.setOnClickListener {
            val intent = Intent(this, MapSelectActivity::class.java)
            mapLauncher.launch(intent)
            toggleFab()
        }

        // 챌린지 등록 버튼
        binding.saveButton.setOnClickListener {
            uploadPost()
        }
    }
    // FAB 메뉴를 열고 닫는 함수
    private fun toggleFab() {
        if (isFabOpen) {
            // 메뉴 닫기
            binding.fabMain.setImageResource(R.drawable.ic_add)
            binding.fabAddImage.startAnimation(fabClose)
            binding.fabAddLocation.startAnimation(fabClose)
            binding.fabAddImage.isClickable = false
            binding.fabAddLocation.isClickable = false
        } else {
            // 메뉴 열기

            binding.fabMain.setImageResource(R.drawable.ic_close)
            binding.fabAddImage.startAnimation(fabOpen)
            binding.fabAddLocation.startAnimation(fabOpen)
            binding.fabAddImage.isClickable = true
            binding.fabAddLocation.isClickable = true
        }
        isFabOpen = !isFabOpen
    }
    // 게시글 업로드 함수
    private fun uploadPost() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.contentEditText.text.toString().trim()
        val currentUserUid = auth.currentUser?.uid

        // 유효성 검사
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUserUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return // 혹은 로그인 화면으로 이동
        }

        binding.saveButton.isEnabled = false // 중복 클릭 방지

        // 이미지가 있는 경우와 없는 경우를 분기
        if (imageUri != null) {
            uploadImageAndSavePost(currentUserUid, title, description)
        } else {
            savePostToFirestore(currentUserUid, title, description, null)
        }
    }
    // 1. 이미지를 Storage에 업로드 후 Firestore에 저장
    private fun uploadImageAndSavePost(uid: String, title: String, description: String) {
        val fileName = "POST_${System.currentTimeMillis()}.png"
        val imageRef = storage.reference.child("posts/$uid/$fileName")

        imageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    // 이미지 업로드 성공 시, 다운로드 URL과 함께 Firestore에 저장
                    savePostToFirestore(uid, title, description, uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
            }
    }

    // 2. 최종 데이터를 Firestore에 저장
    private fun savePostToFirestore(uid: String, title: String, description: String, imageUrl: String?) {
        val post = hashMapOf<String, Any>(
            "creatorUid" to uid,
            "title" to title,
            "description" to description,
            "createdAt" to Date(),
            "status" to "recruiting" // 초기 상태는 '모집중'
        )

        imageUrl?.let { post["imageUrl"] = it }

        // 위치 정보가 있을 경우에만 맵에 추가
        if (location != null && locationAddress != null) {
            post["location"] = location!!
            post["locationAddress"] = locationAddress!!
        }

        firestore.collection("challenges")
            .add(post)
            .addOnSuccessListener {
                Toast.makeText(this, "챌린지 등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                finish() // 등록 완료 후 액티비티 종료
            }
            .addOnFailureListener {
                Toast.makeText(this, "챌린지 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
            }
    }
}