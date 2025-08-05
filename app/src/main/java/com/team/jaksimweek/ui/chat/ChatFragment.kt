package com.team.jaksimweek.ui // 실제 프로젝트 구조에 맞게 수정

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.team.jaksimweek.adapter.UserSearchAdapter
import com.team.jaksimweek.databinding.FragmentChatBinding
import com.team.jaksimweek.ui.chat.ChatActivity
import com.team.jaksimweek.ui.chat.ChatUtil
import com.team.jaksimweek.viewmodel.ChatViewModel

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    // 1. ViewModel 과 Adapter 인스턴스 생성
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var userSearchAdapter: UserSearchAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 2. 필요한 모든 설정 함수들을 호출
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    /**
     * RecyclerView와 Adapter를 초기화하고 연결합니다.
     */
    private fun setupRecyclerView() {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid
            ?: return
        userSearchAdapter = UserSearchAdapter { user ->
            Log.d("ChatDebug", "▶️ ChatFragment onChatClick for user=${user.uid}")
            ChatUtil.createOrGetOneToOneChatRoom(myUid, user.uid) { roomId ->
                Log.d("ChatDebug", "🟢 ChatUtil callback, roomId=$roomId")
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    putExtra("roomId", roomId)
                    putExtra("roomType", "1on1")
                    putExtra("roomName", user.nickname)
                }
                startActivity(intent)
            }
        }

        binding.rvSearchResults.apply {
            adapter = userSearchAdapter
            layoutManager = LinearLayoutManager(requireContext())
        }
    }

    /**
     * UI 요소들의 클릭 이벤트를 설정합니다.
     */
    private fun setupClickListeners() {
        // '채팅 시작' 버튼 클릭 시
        binding.btnStartChatFlow.setOnClickListener {
            it.visibility = View.GONE
            binding.layoutSearchUser.visibility = View.VISIBLE
            binding.rvSearchResults.visibility = View.GONE
        }

        // '검색' 버튼 클릭 시
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                // 3. ViewModel의 검색 함수를 실제로 호출
                viewModel.searchUser(query)
                binding.layoutSearchUser.visibility = View.GONE
                binding.rvSearchResults.visibility = View.VISIBLE
            } else {
                Toast.makeText(requireContext(), "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * ViewModel의 LiveData를 관찰하여 UI를 업데이트합니다.
     */
    private fun observeViewModel() {
        // 검색 결과를 관찰하고, 변경이 생기면 Adapter에 데이터를 전달
        viewModel.searchResults.observe(viewLifecycleOwner) { users ->
            userSearchAdapter.submitList(users)
            if (users.isEmpty()) {
                Toast.makeText(requireContext(), "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        // 에러 메시지를 관찰
        viewModel.errorMessage.observe(viewLifecycleOwner) { message ->
            Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // 메모리 누수 방지
    }
}