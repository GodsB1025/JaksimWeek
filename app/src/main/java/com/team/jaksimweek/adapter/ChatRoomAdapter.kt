package com.team.jaksimweek.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.jaksimweek.databinding.ItemChatRoomBinding // ‼️ 뷰바인딩 클래스 (아래 XML 생성 필요)
import com.team.jaksimweek.data.model.ChatRoom
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide
import com.team.jaksimweek.R

class ChatRoomAdapter(private val onItemClick: (ChatRoom) -> Unit) :
    ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatRoomViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatRoomViewHolder(
        private val binding: ItemChatRoomBinding,
        private val onItemClick: (ChatRoom) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatRoom: ChatRoom) {
            binding.tvLastMessage.text = chatRoom.lastMessage
            binding.tvTimestamp.text = formatTimestamp(chatRoom.lastMessageTimestamp)

            if (chatRoom.type == "1on1") {
                binding.tvRoomName.text = chatRoom.partnerNickname ?: "대화 상대"
                Glide.with(itemView.context)
                    .load(chatRoom.partnerProfileImageUrl)
                    .placeholder(R.drawable.ic_person)
                    .circleCrop()
                    .into(binding.ivRoomIcon)
            } else {
                binding.tvRoomName.text = chatRoom.roomName ?: "그룹 채팅"
                binding.ivRoomIcon.setImageResource(R.drawable.ic_group)
            }

            itemView.setOnClickListener {
                onItemClick(chatRoom)
            }
        }

        private fun formatTimestamp(timestamp: Long): String {
            if (timestamp == 0L) return ""
            val sdf = SimpleDateFormat("MM/dd HH:mm", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }
    }

    private class ChatRoomDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem.roomId == newItem.roomId
        }

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom): Boolean {
            return oldItem == newItem
        }
    }
}