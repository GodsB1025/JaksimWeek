package com.team.jaksimweek.ui.chat

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.team.jaksimweek.adapter.UserSearchAdapter
import com.team.jaksimweek.databinding.ActivityUserSearchBinding
import com.team.jaksimweek.util.ChatUtil
import com.team.jaksimweek.viewmodel.ChatViewModel

class UserSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserSearchBinding
    private val viewModel: ChatViewModel by viewModels()
    private lateinit var userSearchAdapter: UserSearchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        val myUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        userSearchAdapter = UserSearchAdapter { user ->
            Log.d("ChatDebug", "▶️ UserSearchActivity onChatClick for user=${user.uid}")
            ChatUtil.createOrGetOneToOneChatRoom(myUid, user.uid) { roomId ->
                Log.d("ChatDebug", "🟢 ChatUtil callback, roomId=$roomId")
                val intent = Intent(this, ChatActivity::class.java).apply {
                    putExtra("roomId", roomId)
                    putExtra("roomType", "1on1")
                    putExtra("roomName", user.nickname)
                }
                startActivity(intent)
                finish()
            }
        }

        binding.rvSearchResults.apply {
            adapter = userSearchAdapter
            layoutManager = LinearLayoutManager(this@UserSearchActivity)
        }
    }

    private fun setupClickListeners() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearchQuery.text.toString().trim()
            if (query.isNotEmpty()) {
                viewModel.searchUser(query)
            } else {
                Toast.makeText(this, "검색어를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun observeViewModel() {
        viewModel.searchResults.observe(this) { users ->
            userSearchAdapter.submitList(users)
            if (users.isEmpty()) {
                Toast.makeText(this, "검색 결과가 없습니다.", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.errorMessage.observe(this) { message ->
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        }
    }
}