package com.team.jaksimweek.adapter // 실제 프로젝트 구조에 맞게 수정

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.team.jaksimweek.data.model.User // 제공해주신 User 데이터 클래스
import com.team.jaksimweek.databinding.ItemUserSearchBinding // ViewBinding 클래스

class UserSearchAdapter(
    private val onChatClick: (User) -> Unit
) : ListAdapter<User, UserSearchAdapter.UserViewHolder>(UserDiffCallback()) {

    class UserViewHolder(
        private val binding: ItemUserSearchBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User, onChatClick: (User) -> Unit) {
            binding.tvNickname.text = user.nickname ?: "알 수 없는 사용자"
            binding.btnStartChat.setOnClickListener {
                Log.d("ChatDebug", "💬 btnStartChat clicked for ${user.uid}")
                onChatClick(user)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemUserSearchBinding.inflate(inflater, parent, false)
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val currentUser = getItem(position)
        holder.bind(currentUser, onChatClick)
    }

    class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.uid == newItem.uid
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}