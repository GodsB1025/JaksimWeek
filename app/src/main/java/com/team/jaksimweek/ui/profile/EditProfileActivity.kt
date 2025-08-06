package com.team.jaksimweek.ui.profile

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.R
import com.team.jaksimweek.data.model.User
import com.team.jaksimweek.databinding.ActivityProfileBinding
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var selectedImageUri: Uri? = null
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
                selectedImageUri = it
                Glide.with(this)
                    .load(it)
                    .circleCrop()
                    .into(binding.ivProfile)
            }
        }
    }

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .into(binding.ivProfile)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadUserData()

        binding.ivProfile.setOnClickListener {
            showImageSelectionDialog()
        }

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

                    Glide.with(this)
                        .load(user.profileImageUrl)
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .circleCrop()
                        .into(binding.ivProfile)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "사용자 정보 로딩 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showImageSelectionDialog() {
        val options = arrayOf("카메라로 촬영하기", "갤러리에서 선택하기")
        AlertDialog.Builder(this)
            .setTitle("프로필 사진 선택")
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

    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return
        val nickname = binding.etNickname.text.toString()
        val bio = binding.etBio.text.toString()

        if (selectedImageUri != null) {
            val imageRef = storage.reference.child("profile_images/$uid.jpg")
            imageRef.putFile(selectedImageUri!!)
                .continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    imageRef.downloadUrl
                }
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val downloadUrl = task.result.toString()
                        updateUserInfo(uid, nickname, bio, downloadUrl)
                    } else {
                        Toast.makeText(this, "이미지 업로드 실패", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
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
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "업데이트 실패", Toast.LENGTH_SHORT).show()
            }
    }
}