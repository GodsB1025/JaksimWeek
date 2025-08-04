package com.team.jaksimweek.ui.challenge

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.team.jaksimweek.R
import com.team.jaksimweek.databinding.FragmentCommentsBinding

class CommentsFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentCommentsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCommentsBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val commentRecyclerView = binding.commentRecyclerView

        ViewCompat.setOnApplyWindowInsetsListener(binding.commentsLayout) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val tempDataset = ArrayList<CommentVO>()
        tempDataset.add(CommentVO("닉네임", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임2", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임3", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임4", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임5", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임6", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임7", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임8", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임9", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))
        tempDataset.add(CommentVO("닉네임10", "Lorem ipsum dolor sit amet, consectetur adipisg eli", R.drawable.temp))

        commentRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        commentRecyclerView.adapter = CommentAdapter(requireContext(), tempDataset)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        // 프래그먼트 뷰가 파괴될 때 바인딩 참조 해제 (메모리 누수 방지)
        _binding = null
    }

}