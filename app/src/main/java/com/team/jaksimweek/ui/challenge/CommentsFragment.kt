package com.team.jaksimweek.ui.challenge

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.team.jaksimweek.adapter.CommentAdapter
import com.team.jaksimweek.data.model.Comment
import com.team.jaksimweek.databinding.FragmentCommentsBinding

class CommentsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    private lateinit var commentAdapter: CommentAdapter
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private val auth by lazy { FirebaseAuth.getInstance() }

    private var challengeId: String? = null

    // newInstance 패턴을 사용하여 challengeId를 받습니다.
    companion object {
        private const val ARG_CHALLENGE_ID = "challenge_id"

        fun newInstance(challengeId: String): CommentsFragment {
            val fragment = CommentsFragment()
            val args = Bundle()
            args.putString(ARG_CHALLENGE_ID, challengeId)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            challengeId = it.getString(ARG_CHALLENGE_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        loadComments()

        // 댓글 등록 버튼 클릭 리스너 설정
        binding.btnComment.setOnClickListener {
            val content = binding.inputComment.text.toString().trim()
            if (content.isNotEmpty()) {
                addComment(content)
            } else {
                Toast.makeText(context, "댓글을 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupRecyclerView() {
        commentAdapter = CommentAdapter(emptyList())
        binding.commentRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = commentAdapter
        }
    }

    // Firestore에서 댓글을 불러오는 함수
    private fun loadComments() {
        if (challengeId == null) return

        firestore.collection("challenges").document(challengeId!!)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING) // 시간순으로 정렬
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("CommentsFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val comments = snapshots.toObjects(Comment::class.java)
                    commentAdapter.updateData(comments)
                    binding.commentRecyclerView.scrollToPosition(comments.size - 1)
                }
            }
    }

    // 새로운 댓글을 Firestore에 추가하는 함수
    private fun addComment(content: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (challengeId == null) return

        // 사용자 닉네임을 가져오기 위해 'users' 컬렉션 조회
        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDocument ->
                val nickname = userDocument.getString("nickname") ?: "익명"

                val comment = Comment(
                    writerUid = currentUser.uid,
                    writerNickname = nickname,
                    content = content
                )

                firestore.collection("challenges").document(challengeId!!)
                    .collection("comments")
                    .add(comment)
                    .addOnSuccessListener {
                        binding.inputComment.text.clear() // 입력창 초기화
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(context, "댓글 등록에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "사용자 정보 조회에 실패했습니다: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}