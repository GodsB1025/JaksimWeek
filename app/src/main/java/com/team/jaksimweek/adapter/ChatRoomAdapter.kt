package com.team.jaksimweek.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.team.jaksimweek.R
import com.team.jaksimweek.data.model.ChatRoom
import com.team.jaksimweek.databinding.ItemChatRoomBinding
import java.text.SimpleDateFormat
import java.util.*

class ChatRoomAdapter(private val onItemClick: (ChatRoom) -> Unit) :
    ListAdapter<ChatRoom, ChatRoomAdapter.ChatRoomViewHolder>(ChatRoomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatRoomViewHolder {
        val binding = ItemChatRoomBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChatRoomViewHolder(binding, onItemClick)
    }

    override fun onBindViewHolder(holder: ChatRoomViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    fun getRoomName(chatRoom: ChatRoom): String {
        return if (chatRoom.type == "1on1") {
            chatRoom.partnerNickname ?: "대화 상대"
        } else {
            chatRoom.roomName ?: "그룹 채팅"
        }
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