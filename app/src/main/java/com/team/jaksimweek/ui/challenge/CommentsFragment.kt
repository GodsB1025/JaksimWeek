package com.team.jaksimweek.ui.challenge

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetBehavior
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

        dialog?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)

        view.viewTreeObserver.addOnGlobalLayoutListener {
            val bottomSheet = dialog?.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val behavior = BottomSheetBehavior.from(bottomSheet)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.peekHeight = 0
            }
        }

        setupRecyclerView()
        loadComments()

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
    private fun loadComments() {
        if (challengeId == null) return

        firestore.collection("challenges").document(challengeId!!)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, error ->
                if (error != null) {
                    Log.w("CommentsFragment", "Listen failed.", error)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val comments = snapshots.toObjects(Comment::class.java)
                    commentAdapter.updateData(comments)
                    if (comments.isNotEmpty()) {
                        binding.commentRecyclerView.scrollToPosition(comments.size - 1)
                    }
                }
            }
    }
    private fun addComment(content: String) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }
        if (challengeId == null) return

        firestore.collection("users").document(currentUser.uid).get()
            .addOnSuccessListener { userDocument ->
                val nickname = userDocument.getString("nickname") ?: "익명"
                val profileImageUrl = userDocument.getString("profileImageUrl")

                val comment = Comment(
                    writerUid = currentUser.uid,
                    writerNickname = nickname,
                    writerProfileImageUrl = profileImageUrl,
                    content = content
                )

                firestore.collection("challenges").document(challengeId!!)
                    .collection("comments")
                    .add(comment)
                    .addOnSuccessListener {
                        binding.inputComment.text.clear()
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