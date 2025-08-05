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
        loadMyChallenges()
    }

    private fun setupRecyclerView() {
        challengeAdapter = ChallengeAdapter(emptyList())
        binding.rvMyChallenges.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = challengeAdapter
        }
        challengeAdapter.setOnItemClickListener(object : ChallengeAdapter.OnItemClickListener {
            override fun onItemClick(challenge: Challenge) {
                val intent = Intent(requireContext(), ChallengeDetailActivity::class.java)
                intent.putExtra("CHALLENGE_ID", challenge.id)
                startActivity(intent)
            }
        })
    }

    private fun loadMyChallenges() {
        val uid = auth.currentUser?.uid ?: return

        // 참고: Firestore는 'whereEqualTo'와 다른 필드의 'orderBy'를 함께 사용하는 쿼리에 복합 색인을 요구합니다.
        // 색인 부재로 인한 앱 충돌을 방지하기 위해 orderBy를 제거하고, 데이터를 가져온 후 앱에서 직접 정렬합니다.
        // 가장 좋은 방법은 Logcat의 링크를 통해 Firebase 콘솔에서 색인을 생성하는 것입니다.
        firestore.collection("challenges")
            .whereEqualTo("creatorUid", uid)
            // .orderBy("createdAt", Query.Direction.DESCENDING) // 색인 없이는 이 줄이 오류를 유발합니다.
            .get()
            .addOnSuccessListener { documents ->
                val myChallenges = documents.toObjects(Challenge::class.java)
                // 앱에서 직접 리스트를 최신순으로 정렬합니다.
                val sortedChallenges = myChallenges.sortedByDescending { it.createdAt }
                challengeAdapter.updateData(sortedChallenges)
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "내 챌린지 목록 로딩 실패", Toast.LENGTH_SHORT).show()
                Log.e("ProfileFragment", "내 챌린지 목록 로딩 실패", e)
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
                                }
                            }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "데이터 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
