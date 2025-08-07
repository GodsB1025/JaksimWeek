package com.team.jaksimweek.adapter

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
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

    override fun onBindViewHolder(
        holder: ChatRoomViewHolder,
        position: Int,
        payloads: MutableList<Any>
    ) {
        if (payloads.isEmpty()) {
            super.onBindViewHolder(holder, position, payloads)
            return
        }

        val bundle = payloads[0] as Bundle
        if (bundle.containsKey("unreadCount")) {
            holder.updateUnreadCount(bundle.getInt("unreadCount"))
        }
        if (bundle.containsKey("lastMessage")) {
            holder.updateLastMessage(bundle.getString("lastMessage")!!, bundle.getLong("lastMessageTimestamp"))
        }
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
            // Full bind
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

            updateUnreadCount(chatRoom.unreadCount)

            itemView.setOnClickListener {
                onItemClick(chatRoom)
            }
        }

        fun updateUnreadCount(unreadCount: Int) {
            if (unreadCount > 0) {
                binding.tvUnreadCount.text = unreadCount.toString()
                binding.tvUnreadCount.visibility = View.VISIBLE
            } else {
                binding.tvUnreadCount.visibility = View.GONE
            }
        }

        fun updateLastMessage(message: String, timestamp: Long) {
            binding.tvLastMessage.text = message
            binding.tvTimestamp.text = formatTimestamp(timestamp)
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

        override fun getChangePayload(oldItem: ChatRoom, newItem: ChatRoom): Any? {
            val diffBundle = Bundle()

            if (oldItem.unreadCount != newItem.unreadCount) {
                diffBundle.putInt("unreadCount", newItem.unreadCount)
            }
            if (oldItem.lastMessage != newItem.lastMessage || oldItem.lastMessageTimestamp != newItem.lastMessageTimestamp) {
                diffBundle.putString("lastMessage", newItem.lastMessage)
                diffBundle.putLong("lastMessageTimestamp", newItem.lastMessageTimestamp)
            }

            return if (diffBundle.isEmpty) null else diffBundle
        }
    }
}