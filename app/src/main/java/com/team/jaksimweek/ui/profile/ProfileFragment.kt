package com.team.jaksimweek.ui.profile

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.team.jaksimweek.data.model.User
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.databinding.FragmentProfileBinding
import com.team.jaksimweek.ui.profile.EditProfileActivity
import com.team.jaksimweek.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
    }
    override fun onResume() {
        super.onResume()
        // 이 프래그먼트가 화면에 보일 때마다 사용자 정보를 새로고침합니다.
        loadUserData()
    }

    private fun setupClickListeners() {
        // 프로필 수정 버튼 클릭
        binding.btnEditProfile.setOnClickListener {
            startActivity(Intent(requireContext(), EditProfileActivity::class.java))
        }

        // 로그아웃 버튼 클릭
        binding.btnLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(context, "로그아웃되었습니다.", Toast.LENGTH_SHORT).show()
            val intent = Intent(requireContext(), LoginActivity::class.java)
            // 모든 이전 화면을 스택에서 제거하고 로그인 화면을 새 시작점으로 만듭니다.
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // 회원 탈퇴 버튼 클릭
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
            .setPositiveButton("탈퇴") { _, _ ->
                withdrawUser()
            }
            .setNegativeButton("취소", null)
            .show()
    }
    private fun withdrawUser() {
        val uid = auth.currentUser?.uid ?: return

        // 1. Storage의 프로필 이미지 삭제
        storage.reference.child("profile_images/$uid.jpg").delete()
            .addOnCompleteListener {
                // Storage 삭제 성공 여부와 상관없이 Firestore 삭제 진행
                // (프로필 이미지가 없는 사용자일 수도 있기 때문)

                // 2. Firestore의 user 정보 삭제
                firestore.collection("users").document(uid).delete()
                    .addOnSuccessListener {

                        // 3. Authentication의 유저 정보 삭제 (가장 마지막에 수행)
                        auth.currentUser?.delete()
                            ?.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                                    // 로그아웃 처리 및 로그인 화면으로 이동
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
        _binding = null // 메모리 누수 방지
    }
}