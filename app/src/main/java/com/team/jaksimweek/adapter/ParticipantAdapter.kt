package com.team.jaksimweek.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.team.jaksimweek.R
import com.team.jaksimweek.data.model.User
import com.team.jaksimweek.databinding.ItemParticipantBinding

class ParticipantAdapter(private val users: List<User>) : RecyclerView.Adapter<ParticipantAdapter.ParticipantViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ParticipantViewHolder {
        val binding = ItemParticipantBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ParticipantViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ParticipantViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount(): Int = users.size

    class ParticipantViewHolder(private val binding: ItemParticipantBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(user: User) {
            binding.tvParticipantNickname.text = user.nickname
            Glide.with(itemView.context)
                .load(user.profileImageUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .into(binding.ivParticipantProfile)
        }
    }
}