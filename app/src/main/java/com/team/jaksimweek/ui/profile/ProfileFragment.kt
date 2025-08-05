package com.team.jaksimweek.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.R
import com.team.jaksimweek.adapter.ChallengeAdapter
import com.team.jaksimweek.data.model.Challenge
import com.team.jaksimweek.data.model.User
import com.team.jaksimweek.databinding.FragmentProfileBinding
import com.team.jaksimweek.ui.auth.LoginActivity
import com.team.jaksimweek.ui.challenge.ChallengeDetailActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private lateinit var challengeAdapter: ChallengeAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        // 화면에 처음 진입 시 '내가 작성한 챌린지'를 기본으로 로드
        loadMyChallenges()
    }

    private fun setupRecyclerView() {
        // 1. 어댑터를 초기화합니다. (기존 어댑터 구조에 맞게)
        challengeAdapter = ChallengeAdapter(emptyList())

        // 2. 어댑터에 정의된 setOnItemClickListener를 사용하여 리스너를 설정합니다.
        challengeAdapter.setOnItemClickListener(object : ChallengeAdapter.OnItemClickListener {
            override fun onItemClick(challenge: Challenge) {
                // 아이템 클릭 시 상세 페이지로 이동
                val intent = Intent(requireContext(), ChallengeDetailActivity::class.java)
                intent.putExtra("challenge_id", challenge.id)
                startActivity(intent)
            }
        })

        // 3. RecyclerView에 어댑터와 레이아웃 매니저를 연결합니다.
        binding.rvChallenges.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = challengeAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }
        binding.btnWithdraw.setOnClickListener {
            showWithdrawConfirmationDialog()
        }

        // 필터 버튼 클릭 리스너
        binding.btnMyChallenge.setOnClickListener {
            loadMyChallenges()
        }
        binding.btnFavoriteChallenge.setOnClickListener {
            loadFavoriteChallenges()
        }
        binding.btnBookmarkChallenge.setOnClickListener {
            loadBookmarkedChallenges()
        }
    }

    // 내가 작성한 챌린지 로드
    private fun loadMyChallenges() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("challenges")
            .whereEqualTo("creatorUid", uid)
            .get()
            .addOnSuccessListener { documents ->
                val myChallenges = documents.toObjects(Challenge::class.java)
                challengeAdapter.updateData(myChallenges.sortedByDescending { it.createdAt })
                Log.d("ProfileFragment", "내가 작성한 챌린지 ${myChallenges.size}개 로드 성공")
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "내 챌린지 목록 로딩 실패", Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "내 챌린지 목록 로딩 실패", e)
            }
    }

    // 좋아요 누른 챌린지 로드
    private fun loadFavoriteChallenges() {
        val uid = auth.currentUser?.uid ?: return

        // 1. 현재 유저의 정보를 가져옵니다.
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                val likedIds = user?.likedChallengeIds // 2. 좋아요 누른 챌린지 ID 목록을 가져옵니다.

                if (likedIds.isNullOrEmpty()) {
                    challengeAdapter.updateData(emptyList()) // 목록이 없으면 리스트를 비웁니다.
                    Log.d("ProfileFragment", "좋아요 챌린지가 없습니다.")
                    return@addOnSuccessListener
                }

                // 3. ID 목록을 사용하여 'challenges' 컬렉션에서 해당 챌린지들을 가져옵니다.
                firestore.collection("challenges").whereIn("id", likedIds)
                    .get()
                    .addOnSuccessListener { challengesSnapshot ->
                        val challenges = challengesSnapshot.toObjects(Challenge::class.java)
                        challengeAdapter.updateData(challenges.sortedByDescending { it.createdAt })
                        Log.d("ProfileFragment", "좋아요 챌린지 ${challenges.size}개 로드 성공")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "좋아요 챌린지 목록 로딩 실패", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "좋아요 챌린지 whereIn 쿼리 실패", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "사용자 정보 로딩 실패", Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "좋아요 챌린지를 위한 유저 정보 로딩 실패", e)
            }
    }

    // 북마크한 챌린지 로드
    private fun loadBookmarkedChallenges() {
        val uid = auth.currentUser?.uid ?: return

        // 1. 현재 유저의 정보를 가져옵니다.
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(User::class.java)
                val bookmarkedIds = user?.bookmarkedChallengeIds // 2. 북마크한 챌린지 ID 목록을 가져옵니다.

                if (bookmarkedIds.isNullOrEmpty()) {
                    challengeAdapter.updateData(emptyList()) // 목록이 없으면 리스트를 비웁니다.
                    Log.d("ProfileFragment", "북마크 챌린지가 없습니다.")
                    return@addOnSuccessListener
                }

                // 3. ID 목록을 사용하여 'challenges' 컬렉션에서 해당 챌린지들을 가져옵니다.
                firestore.collection("challenges").whereIn("id", bookmarkedIds)
                    .get()
                    .addOnSuccessListener { challengesSnapshot ->
                        val challenges = challengesSnapshot.toObjects(Challenge::class.java)
                        challengeAdapter.updateData(challenges.sortedByDescending { it.createdAt })
                        Log.d("ProfileFragment", "북마크 챌린지 ${challenges.size}개 로드 성공")
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "북마크 챌린지 목록 로딩 실패", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "북마크 챌린지 whereIn 쿼리 실패", e)
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "사용자 정보 로딩 실패", Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "북마크 챌린지를 위한 유저 정보 로딩 실패", e)
            }
    }

    private fun loadUserData() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val user = document.toObject(User::class.java)
                    user?.let {
                        binding.tvNickname.text = it.nickname
                        binding.tvEmail.text = it.email
                        binding.tvBio.text = it.bio

                        Glide.with(this)
                            .load(it.profileImageUrl)
                            .placeholder(R.drawable.ic_person)
                            .error(R.drawable.ic_person)
                            .circleCrop()
                            .into(binding.ivProfile)
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(context, "사용자 정보 로딩에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showWithdrawConfirmationDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("회원 탈퇴")
            .setMessage("정말로 탈퇴하시겠습니까? 모든 정보가 삭제되며 되돌릴 수 없습니다.")
            .setPositiveButton("탈퇴") { _, _ -> withdrawUser() }
            .setNegativeButton("취소", null)
            .show()
    }


    private fun withdrawUser() {
        val uid = auth.currentUser?.uid ?: return
        // 프로필 이미지가 없을 수도 있으므로 삭제 실패는 무시하고 계속 진행합니다.
        storage.reference.child("profile_images/$uid.jpg").delete()
            .addOnCompleteListener {
                firestore.collection("users").document(uid).delete()
                    .addOnSuccessListener {
                        auth.currentUser?.delete()
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                    val intent = Intent(requireContext(), LoginActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(context, "탈퇴 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                                    Log.e("ProfileFragment", "Auth 삭제 실패", task.exception)
                                }
                            }
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "데이터 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                        Log.e("ProfileFragment", "Firestore 삭제 실패", e)
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}