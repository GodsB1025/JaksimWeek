package com.team.jaksimweek.ui.challenge

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.R
import com.team.jaksimweek.data.model.Location
import com.team.jaksimweek.databinding.ActivityAddPostBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AddPostActivity : AppCompatActivity() {
    private lateinit var binding: ActivityAddPostBinding

    private val auth: FirebaseAuth by lazy { FirebaseAuth.getInstance() }
    private val storage: FirebaseStorage by lazy { FirebaseStorage.getInstance() }
    private val firestore: FirebaseFirestore by lazy { FirebaseFirestore.getInstance() }

    private var imageUri: Uri? = null
    private var photoUri: Uri? = null
    private var selectedLocation: Location? = null

    private val fabOpen: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_open) }
    private val fabClose: Animation by lazy { AnimationUtils.loadAnimation(this, R.anim.fab_close) }
    private var isFabOpen = false

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
                imageUri = it
                binding.postImageView.setImageURI(it)
                binding.postImageView.visibility = View.VISIBLE
            }
        }
    }

    private val galleryLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageUri = uri
                binding.postImageView.setImageURI(uri)
                binding.postImageView.visibility = View.VISIBLE
            }
        }

    private val mapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val lat = result.data?.getDoubleExtra("latitude", 0.0)
                val lng = result.data?.getDoubleExtra("longitude", 0.0)
                val address = result.data?.getStringExtra("address")

                if (lat != null && lng != null && address != null) {
                    selectedLocation = Location(
                        latitude = lat,
                        longitude = lng,
                        addressName = address
                    )
                    binding.locationTextView.text = "📍 $address"
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupClickListeners()

        setSupportActionBar(binding.toolbar)
    }

    private fun setupClickListeners() {
        binding.fabMain.setOnClickListener { toggleFab() }
        binding.fabAddImage.setOnClickListener {
            showImageSelectionDialog()
            toggleFab()
        }
        binding.fabAddLocation.setOnClickListener {
            val intent = Intent(this, MapSelectActivity::class.java)
            mapLauncher.launch(intent)
            toggleFab()
        }
        binding.saveButton.setOnClickListener { uploadPost() }
    }

    private fun toggleFab() {
        if (isFabOpen) {
            binding.fabMain.setImageResource(R.drawable.ic_add)
            binding.fabAddImage.startAnimation(fabClose)
            binding.fabAddLocation.startAnimation(fabClose)
            binding.fabAddImage.isClickable = false
            binding.fabAddLocation.isClickable = false
        } else {
            binding.fabMain.setImageResource(R.drawable.ic_close)
            binding.fabAddImage.startAnimation(fabOpen)
            binding.fabAddLocation.startAnimation(fabOpen)
            binding.fabAddImage.isClickable = true
            binding.fabAddLocation.isClickable = true
        }
        isFabOpen = !isFabOpen
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("카메라로 촬영하기", "갤러리에서 선택하기")
        AlertDialog.Builder(this)
            .setTitle("이미지 선택")
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

    private fun uploadPost() {
        val title = binding.titleEditText.text.toString().trim()
        val description = binding.contentEditText.text.toString().trim()
        val currentUser = auth.currentUser

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(this, "제목과 내용을 입력해주세요.", Toast.LENGTH_SHORT).show()
            return
        }
        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.saveButton.isEnabled = false

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDocument ->
                val userNickname = userDocument.getString("nickname") ?: "익명"

                if (imageUri != null) {
                    uploadImageAndSavePost(currentUser.uid, userNickname, title, description)
                } else {
                    savePostToFirestore(currentUser.uid, userNickname, title, description, null)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 정보를 가져오는 데 실패했습니다.", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
            }
    }

    private fun uploadImageAndSavePost(uid: String, nickname: String, title: String, description: String) {
        val fileName = "POST_${System.currentTimeMillis()}.png"
        val imageRef = storage.reference.child("posts/$uid/$fileName")

        imageRef.putFile(imageUri!!)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    savePostToFirestore(uid, nickname, title, description, uri.toString())
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "이미지 업로드에 실패했습니다.", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
            }
    }

    private fun savePostToFirestore(uid: String, nickname: String, title: String, description: String, imageUrl: String?) {
        val newChallengeRef = firestore.collection("challenges").document()

        val post = hashMapOf<String, Any?>(
            "id" to newChallengeRef.id,
            "creatorUid" to uid,
            "creatorNickname" to nickname,
            "title" to title,
            "description" to description,
            "imageUrl" to imageUrl,
            "status" to "recruiting",
            "participantCount" to 1,
            "likeCount" to 0,
            "createdAt" to Timestamp.now(),
            "location" to selectedLocation
        )

        newChallengeRef.set(post)
            .addOnSuccessListener {
                Toast.makeText(this, "챌린지 등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, ChallengeDetailActivity::class.java)
                intent.putExtra("CHALLENGE_ID", newChallengeRef.id)
                startActivity(intent)

                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "챌린지 등록에 실패했습니다.", Toast.LENGTH_SHORT).show()
                binding.saveButton.isEnabled = true
            }
    }
}