package com.team.jaksimweek.ui.challenge

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
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
import com.team.jaksimweek.util.ChatUtil

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
    private var isParticipant = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChallengeDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        challengeId = intent.getStringExtra("CHALLENGE_ID")

        if (challengeId == null) {
            Toast.makeText(this, "챌린지 정보를 불러올 수 없습니다.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        binding.btnCommentsHandle.setOnClickListener {
            val commentsFragment = CommentsFragment.newInstance(challengeId!!)
            commentsFragment.show(supportFragmentManager, commentsFragment.tag)
        }

        binding.btnBookMark.setOnClickListener {
            toggleBookmark()
        }

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
                if (isFinishing || isDestroyed) return@addSnapshotListener
                if (error != null) {
                    Toast.makeText(this, "정보 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (document != null && document.exists()) {
                    currentChallenge = document.toObject(Challenge::class.java)
                    currentChallenge?.let {
                        val currentUserUid = auth.currentUser?.uid
                        isCreator = currentUserUid == it.creatorUid
                        isParticipant = it.participantUids.contains(currentUserUid)

                        setupUI(it)
                        loadBookmarkStatus()
                        loadFavoriteStatus()
                    }
                } else {
                    Toast.makeText(this, "삭제되었거나 없는 챌린지입니다.", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
    }
    private fun loadCommentCount() {
        if (challengeId == null) return
        val commentCount = binding.commentCount

        firestore.collection("challenges").document(challengeId!!)
            .collection("comments")
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    binding.commentCount.text = "-"
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    binding.commentCount.text = "${snapshots.size()}"
                } else {
                    binding.commentCount.text = "0"
                }
            }
    }

    private fun setupUI(challenge: Challenge) {
        binding.toolbar.title = challenge.title
        binding.tvChallengeTitle.text = challenge.title
        binding.tvCreatorNickname.text = "게시자: ${challenge.creatorNickname ?: "정보 없음"}"
        binding.tvParticipantCount.text = "참여 인원: ${challenge.participantCount}명"
        binding.tvChallengeDescription.text = challenge.description
        binding.tvLikeCount.text = challenge.likeCount.toString()

        if (challenge.location != null) {
            binding.tvLocation.text = challenge.location.addressName
            binding.tvLocation.visibility = View.VISIBLE
            binding.tvLocation.setOnClickListener {
                val intent = Intent(this, MapSelectActivity::class.java).apply {
                    putExtra("latitude", challenge.location.latitude)
                    putExtra("longitude", challenge.location.longitude)
                    putExtra("address", challenge.location.addressName)
                    putExtra("displayOnly", true)
                }
                startActivity(intent)
            }
        } else {
            binding.tvLocation.visibility = View.GONE
        }

        Glide.with(this)
            .load(challenge.imageUrl)
            .placeholder(R.drawable.edittext_background)
            .into(binding.ivChallengeImage)

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

        val scaleDown = AnimationUtils.loadAnimation(this, R.anim.icon_scale_down)
        favoriteButton.startAnimation(scaleDown)
        scaleDown.setAnimationListener(object : android.view.animation.Animation.AnimationListener {
            override fun onAnimationStart(animation: android.view.animation.Animation?) {}

            override fun onAnimationEnd(animation: android.view.animation.Animation?) {
                val updatedStatus = !isLiked

                if (updatedStatus) {
                    userDocRef.update("likedChallengeIds", FieldValue.arrayUnion(challengeId))
                    challengeDocRef.update("likeCount", FieldValue.increment(1))
                } else {
                    userDocRef.update("likedChallengeIds", FieldValue.arrayRemove(challengeId))
                    challengeDocRef.update("likeCount", FieldValue.increment(-1))
                }

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
        val bookmarkButton = binding.btnBookMark

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
                    updateBookmarkUIWithAnimation(bookmarkButton)
                }.addOnFailureListener {
                    Toast.makeText(this@ChallengeDetailActivity, "북마크 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
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
        val currentUserUid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(currentUserUid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val bookmarkedChallenges = document.get("bookmarkedChallengeIds") as? List<String> ?: emptyList()
                    isBookmarked = bookmarkedChallenges.contains(challengeId)
                    updateBookmarkUI()
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "북마크 정보를 불러오는데 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtonForCreator(challenge: Challenge) {
        binding.btnAction.visibility = View.VISIBLE
        binding.btnAction.isEnabled = true

        when (challenge.status) {
            "recruiting" -> {
                binding.btnAction.text = "모집 완료"
                binding.btnAction.setOnClickListener {
                    showCompleteConfirmationDialog()
                }
            }
            "in-progress" -> {
                binding.btnAction.text = "챌린지 완료"
                binding.btnAction.setOnClickListener {
                    showFinishChallengeDialog()
                }
            }
            "completed" -> {
                binding.btnAction.text = "완료된 챌린지"
                binding.btnAction.isEnabled = false
            }
            else -> {
                binding.btnAction.visibility = View.GONE
            }
        }
    }

    private fun showFinishChallengeDialog() {
        AlertDialog.Builder(this)
            .setTitle("챌린지 완료")
            .setMessage("이 챌린지를 최종 완료 상태로 변경하시겠습니까?")
            .setPositiveButton("완료") { _, _ ->
                updateChallengeStatus("completed")
            }
            .setNegativeButton("취소", null)
            .show()
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

    private fun showCompleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("모집 완료")
            .setMessage("챌린지 모집을 완료하시겠습니까? 참여자들과 함께 그룹 채팅방이 생성됩니다.")
            .setPositiveButton("완료") { _, _ ->
                updateChallengeStatusAndCreateGroupChat()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun updateChallengeStatusAndCreateGroupChat() {
        firestore.collection("challenges").document(challengeId!!)
            .update("status", "in-progress")
            .addOnSuccessListener {
                Toast.makeText(this, "챌린지 상태가 변경되었습니다.", Toast.LENGTH_SHORT).show()

                currentChallenge?.let { challenge ->
                    val allMembers = challenge.participantUids.toMutableList()
                    if (!allMembers.contains(challenge.creatorUid)) {
                        allMembers.add(challenge.creatorUid)
                    }

                    if (allMembers.size > 1) {
                        ChatUtil.createGroupChatRoom(challenge.id, challenge.title, allMembers) { roomId ->
                            Log.d("ChallengeDetail", "그룹 채팅방 생성 완료: $roomId")
                            Toast.makeText(this, "'${challenge.title}' 그룹 채팅방이 생성되었습니다.", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "상태 변경에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setupButtonForParticipant(challenge: Challenge) {
        binding.btnAction.visibility = View.VISIBLE
        if (challenge.status == "recruiting") {
            if (isParticipant) {
                binding.btnAction.text = "참가 취소"
                binding.btnAction.isEnabled = true
                binding.btnAction.setOnClickListener {
                    toggleParticipation(false)
                }
            } else {
                binding.btnAction.text = "참가하기"
                binding.btnAction.isEnabled = true
                binding.btnAction.setOnClickListener {
                    toggleParticipation(true)
                }
            }
        } else {
            binding.btnAction.text = "참가할 수 없는 챌린지입니다"
            binding.btnAction.isEnabled = false
        }
    }

    private fun toggleParticipation(join: Boolean) {
        val currentUserUid = auth.currentUser?.uid
        if (currentUserUid == null) {
            Toast.makeText(this, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        binding.btnAction.isEnabled = false

        val challengeDocRef = firestore.collection("challenges").document(challengeId!!)
        val updateTask = if (join) {
            challengeDocRef.update(
                "participantUids", FieldValue.arrayUnion(currentUserUid),
                "participantCount", FieldValue.increment(1)
            )
        } else {
            challengeDocRef.update(
                "participantUids", FieldValue.arrayRemove(currentUserUid),
                "participantCount", FieldValue.increment(-1)
            )
        }

        updateTask
            .addOnSuccessListener {
                val message = if (join) "챌린지 참여가 완료되었습니다." else "참가를 취소했습니다."
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                binding.btnAction.isEnabled = true
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "작업에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.btnAction.isEnabled = true
            }
    }
}