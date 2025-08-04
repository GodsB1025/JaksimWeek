package com.team.jaksimweek.ui.challenge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.R
import com.team.jaksimweek.data.model.Challenge
import com.team.jaksimweek.databinding.ActivityChallengeDetailBinding

class ChallengeDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChallengeDetailBinding
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var challengeId: String? = null
    private var currentChallenge: Challenge? = null
    private var isCreator = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChallengeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // 툴바 설정 (ViewBinding 사용)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) // 뒤로가기 버튼 활성화

        challengeId = intent.getStringExtra("CHALLENGE_ID")

        if (challengeId == null) {
            Toast.makeText(this, "챌린지 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnCommentHandle.setOnClickListener {
            val commentsFragment = CommentsFragment.newInstance(challengeId!!)
            commentsFragment.show(supportFragmentManager, commentsFragment.tag)
        }

        loadChallengeDetails()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (isCreator) {
            menuInflater.inflate(R.menu.challenge_detail_menu, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            // 뒤로가기 버튼 클릭 처리
            android.R.id.home -> {
                finish()
                true
            }
            R.id.menu_edit_challenge -> {
                val intent = Intent(this, EditChallengeActivity::class.java)
                intent.putExtra("CHALLENGE_ID", challengeId)
                startActivity(intent)
                true
            }
            R.id.menu_delete_challenge -> {
                showDeleteConfirmationDialog()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun loadChallengeDetails() {
        firestore.collection("challenges").document(challengeId!!)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Toast.makeText(this, "정보 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    currentChallenge = document.toObject(Challenge::class.java)
                    currentChallenge?.let { setupUI(it) }
                } else {
                    Toast.makeText(this, "삭제되었거나 없는 챌린지입니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    private fun setupUI(challenge: Challenge) {
        // 툴바 제목을 챌린지 제목으로 설정
        binding.toolbar.title = challenge.title

        binding.tvChallengeTitle.text = challenge.title
        binding.tvCreatorNickname.text = "게시자: ${challenge.creatorNickname ?: "정보 없음"}"
        binding.tvParticipantCount.text = "참여 인원: ${challenge.participantCount}명"
        binding.tvChallengeDescription.text = challenge.description

        if (challenge.location != null) {
            binding.tvLocation.text = challenge.location.addressName
            binding.tvLocation.visibility = View.VISIBLE
        } else {
            binding.tvLocation.visibility = View.GONE
        }

        Glide.with(this)
            .load(challenge.imageUrl)
            .placeholder(R.drawable.edittext_background)
            .into(binding.ivChallengeImage)

        val currentUserUid = auth.currentUser?.uid
        isCreator = currentUserUid == challenge.creatorUid
        invalidateOptionsMenu()

        if (isCreator) {
            setupButtonForCreator(challenge)
        } else {
            setupButtonForParticipant(challenge)
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("삭제 확인")
            .setMessage("정말로 이 챌린지를 삭제하시겠습니까? 되돌릴 수 없습니다.")
            .setPositiveButton("삭제") { _, _ -> deleteChallenge() }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteChallenge() {
        currentChallenge?.imageUrl?.let {
            if (it.isNotEmpty()) {
                storage.getReferenceFromUrl(it).delete()
            }
        }

        firestore.collection("challenges").document(challengeId!!)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "챌린지가 삭제되었습니다.", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "삭제에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtonForCreator(challenge: Challenge) {
        binding.btnAction.visibility = View.VISIBLE
        if (challenge.status == "recruiting") {
            binding.btnAction.text = "모집 완료"
            binding.btnAction.setOnClickListener {
                updateChallengeStatus("completed")
            }
        } else {
            binding.btnAction.text = "모집이 완료된 챌린지입니다"
            binding.btnAction.isEnabled = false
        }
    }

    private fun setupButtonForParticipant(challenge: Challenge) {
        binding.btnAction.visibility = View.VISIBLE
        if (challenge.status == "recruiting") {
            binding.btnAction.text = "참가 요청"
            binding.btnAction.setOnClickListener {
                Toast.makeText(this, "참가 요청이 완료되었습니다 (기능 구현 필요)", Toast.LENGTH_SHORT).show()
            }
        } else {
            binding.btnAction.text = "참가할 수 없는 챌린지입니다"
            binding.btnAction.isEnabled = false
        }
    }


    private fun updateChallengeStatus(newStatus: String) {
        firestore.collection("challenges").document(challengeId!!)
            .update("status", newStatus)
            .addOnSuccessListener {
                Toast.makeText(this, "챌린지 상태가 변경되었습니다.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }
}
