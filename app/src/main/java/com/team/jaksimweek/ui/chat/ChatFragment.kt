package com.team.jaksimweek.ui.chat

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.team.jaksimweek.adapter.ChatRoomAdapter
import com.team.jaksimweek.databinding.FragmentChatBinding
import com.team.jaksimweek.viewmodel.MainViewModel
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!

    private lateinit var chatRoomAdapter: ChatRoomAdapter
    private val viewModel: MainViewModel by activityViewModels()
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val database by lazy { FirebaseDatabase.getInstance().reference }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupSwipeToDelete()

        lifecycleScope.launch {
            viewModel.chatRooms.collect { chatRooms ->
                chatRoomAdapter.submitList(chatRooms)
            }
        }

        binding.btnStartChatFlow.setOnClickListener {
            startActivity(Intent(requireContext(), UserSearchActivity::class.java))
        }
    }

    private fun setupRecyclerView() {
        chatRoomAdapter = ChatRoomAdapter { chatRoom ->
            val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                putExtra("roomId", chatRoom.roomId)
                putExtra("roomType", chatRoom.type)
                putExtra("roomName", chatRoomAdapter.getRoomName(chatRoom))
            }
            startActivity(intent)
        }
        binding.rvChatRooms.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = chatRoomAdapter
        }
    }

    private fun setupSwipeToDelete() {
        val itemTouchHelperCallback = object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.LEFT
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val chatRoomToDelete = chatRoomAdapter.currentList[position]

                AlertDialog.Builder(requireContext())
                    .setTitle("채팅방 나가기")
                    .setMessage("정말로 '${chatRoomAdapter.getRoomName(chatRoomToDelete)}' 채팅방을 나가시겠습니까?\n대화 내용이 모두 삭제됩니다.")
                    .setPositiveButton("나가기") { _, _ ->
                        deleteChatRoomForCurrentUser(chatRoomToDelete.roomId)
                    }
                    .setNegativeButton("취소") { _, _ ->
                        chatRoomAdapter.notifyItemChanged(position)
                    }
                    .setCancelable(false)
                    .show()
            }
        }
        ItemTouchHelper(itemTouchHelperCallback).attachToRecyclerView(binding.rvChatRooms)
    }

    private fun deleteChatRoomForCurrentUser(roomId: String) {
        val myUid = auth.currentUser?.uid ?: return

        database.child("user-chats").child(myUid).child(roomId).removeValue()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}