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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import com.team.jaksimweek.data.model.User
import com.team.jaksimweek.databinding.FragmentProfileBinding
import com.team.jaksimweek.ui.profile.EditProfileActivity
import com.team.jaksimweek.ui.auth.LoginActivity

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val storage by lazy { FirebaseStorage.getInstance() }

    private var userListener: ListenerRegistration? = null

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

    override fun onStart() {
        super.onStart()
        setupSnapshotListener()
    }

    override fun onStop() {
        super.onStop()
        userListener?.remove()
    }

    private fun setupSnapshotListener() {
        val uid = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.contentLayout.visibility = View.INVISIBLE

        val docRef = firestore.collection("users").document(uid)
        userListener = docRef.addSnapshotListener { snapshot, error ->
            binding.progressBar.visibility = View.GONE
            binding.contentLayout.visibility = View.VISIBLE

            if (error != null) {
                Toast.makeText(context, "데이터를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                user?.let {
                    binding.tvNickname.text = it.nickname
                    binding.tvEmail.text = it.email
                    binding.tvBio.text = it.bio.takeIf { bio -> !bio.isNullOrEmpty() } ?: "자기소개를 입력해주세요."

                    Glide.with(this)
                        .load(it.profileImageUrl)
                        .circleCrop()
                        .placeholder(android.R.drawable.sym_def_app_icon) // 이미지가 없을 때 기본 아이콘
                        .into(binding.ivProfile)
                }
            }
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

        storage.reference.child("profile_images/$uid.jpg").delete().addOnCompleteListener {
            firestore.collection("users").document(uid).delete().addOnSuccessListener {
                auth.currentUser?.delete()?.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(context, "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        val intent = Intent(requireContext(), LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                    } else {
                        Toast.makeText(context, "탈퇴 처리 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                Toast.makeText(context, "데이터 삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}