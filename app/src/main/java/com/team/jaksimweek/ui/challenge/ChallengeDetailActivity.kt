package com.team.jaksimweek.ui.challenge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
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
    private var isBookmarked = false
    private var isLiked = false

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

        //댓글창 띄우기
        binding.btnCommentsHandle.setOnClickListener {
            val commentsFragment = CommentsFragment.newInstance(challengeId!!)
            commentsFragment.show(supportFragmentManager, commentsFragment.tag)
        }

        //북마크 토글
        binding.btnBookMark.setOnClickListener {
            toggleBookmark()
        }

        //좋아요 토글
        binding.btnFavorite.setOnClickListener {
            toggleFavorite()
        }

        loadChallengeDetails()
        loadCommentCount()
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
                    currentChallenge?.let {
                        setupUI(it)
                        loadBookmarkStatus() //북마크 상태 불러오기
                        loadFavoriteStatus()    //좋아요 상태 불러오기
                    }
                } else {
                    Toast.makeText(this, "삭제되었거나 없는 챌린지입니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }

    // 댓글 수를 실시간으로 가져와 UI에 업데이트하는 함수
    private fun loadCommentCount() {
        if (challengeId == null) return
        val commentCount = binding.commentCount

        firestore.collection("challenges").document(challengeId!!)
            .collection("comments")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    // 오류 처리
                    binding.commentCount.text = "-"
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    // snapshot의 개수가 바로 댓글의 개수입니다.
                    binding.commentCount.text = "${snapshots.size()}"
                } else {
                    binding.commentCount.text = "0"
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

    private fun toggleFavorite() {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userDocRef = firestore.collection("users").document(currentUserUid)
        val challengeDocRef = firestore.collection("challenges").document(challengeId!!) // 챌린지 문서 참조 추가
        val favoriteButton = binding.btnFavorite

        //애니메이션 코드
        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.icon_scale_down)
        favoriteButton.startAnimation(scaleDown)
        scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val updatedStatus = !isLiked

                // *** 수정된 부분 1: 필드 이름 변경 및 likeCount 업데이트 추가 ***
                if (updatedStatus) {
                    userDocRef.update("likedChallengeIds", FieldValue.arrayUnion(challengeId))
                    challengeDocRef.update("likeCount", FieldValue.increment(1)) // 좋아요 수 증가
                } else {
                    userDocRef.update("likedChallengeIds", FieldValue.arrayRemove(challengeId))
                    challengeDocRef.update("likeCount", FieldValue.increment(-1)) // 좋아요 수 감소
                }

                // 아래 코드는 성공/실패 리스너가 필요 없으므로 바로 실행
                isLiked = updatedStatus
                updateFavoriteUIWithAnimation(favoriteButton)
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }
    private fun updateFavoriteUIWithAnimation(button: ImageButton) {
        updateFavoriteUI()
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.icon_scale_up)
        button.startAnimation(scaleUp)
    }
    private fun updateFavoriteUI() {
        if (isLiked) {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_filled)
        } else {
            binding.btnFavorite.setImageResource(R.drawable.ic_favorite_border)
        }
    }
    private fun loadFavoriteStatus() {
        val currentUserUid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(currentUserUid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val likedChallenges = document.get("likedChallengeIds") as? List<String> ?: emptyList()
                    isLiked = likedChallenges.contains(challengeId)
                    updateFavoriteUI()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "좋아요 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun toggleBookmark() {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val userDocRef = firestore.collection("users").document(currentUserUid)
        val bookmarkButton = binding.btnBookMark // ImageButton 참조

        // 축소 애니메이션 시작
        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.icon_scale_down)
        bookmarkButton.startAnimation(scaleDown)

        scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val updatedStatus = !isBookmarked

                val updateTask = if (updatedStatus) {
                    userDocRef.update("bookmarkedChallengeIds", FieldValue.arrayUnion(challengeId))
                } else {
                    userDocRef.update("bookmarkedChallengeIds", FieldValue.arrayRemove(challengeId))
                }

                updateTask.addOnSuccessListener {
                    isBookmarked = updatedStatus
                    updateBookmarkUIWithAnimation(bookmarkButton) // 아이콘 변경 및 확대 애니메이션 적용
                }.addOnFailureListener {
                    Toast.makeText(this@ChallengeDetailActivity, "북마크 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    // 오류 발생 시 UI를 원래대로 되돌립니다.
                    isBookmarked = !isBookmarked
                    updateBookmarkUI()
                }
            }

            override fun onAnimationRepeat(animation: android.view.animation.Animation?) {}
        })
    }
    private fun updateBookmarkUIWithAnimation(button: ImageButton) {
        if (isBookmarked) {
            button.setImageResource(R.drawable.ic_bookmark_filled)
        } else {
            button.setImageResource(R.drawable.ic_bookmark_border)
        }
        // 확대 애니메이션 시작
        val scaleUp = AnimationUtils.loadAnimation(this, R.anim.icon_scale_up)
        button.startAnimation(scaleUp)
    }
    private fun updateBookmarkUI() {
        if (isBookmarked) {
            binding.btnBookMark.setImageResource(R.drawable.ic_bookmark_filled)
        } else {
            binding.btnBookMark.setImageResource(R.drawable.ic_bookmark_border)
        }
    }
    private fun loadBookmarkStatus() {
        val currentUserUid = auth.currentUser?.uid ?: return // 로그인 상태가 아니면 종료
        firestore.collection("users").document(currentUserUid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    // Firestore의 'bookmarkedChallenges' 필드에서 챌린지 ID 목록을 가져옵니다.
                    val bookmarkedChallenges = document.get("bookmarkedChallengeIds") as? List<String> ?: emptyList()
                    // 현재 챌린지 ID가 목록에 포함되어 있는지 확인하여 isBookmarked 상태를 설정합니다.
                    isBookmarked = bookmarkedChallenges.contains(challengeId)
                    // 상태에 맞게 UI를 업데이트합니다.
                    updateBookmarkUI()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "북마크 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
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
