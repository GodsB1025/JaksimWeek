package com.team.jaksimweek.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.team.jaksimweek.R
import com.team.jaksimweek.data.model.Challenge
import com.team.jaksimweek.databinding.ItemChallengeCardBinding

class ChallengeAdapter(private var challenges: List<Challenge>) :
    RecyclerView.Adapter<ChallengeAdapter.ChallengeViewHolder>() {

    interface OnItemClickListener {
        fun onItemClick(challenge: Challenge)
    }

    private var listener: OnItemClickListener? = null

    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChallengeViewHolder {
        val binding = ItemChallengeCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChallengeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChallengeViewHolder, position: Int) {
        val challenge = challenges[position]
        holder.bind(challenge)
    }

    override fun getItemCount(): Int = challenges.size

    fun updateData(newChallenges: List<Challenge>) {
        challenges = newChallenges
        notifyDataSetChanged()
    }

    inner class ChallengeViewHolder(private val binding: ItemChallengeCardBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    listener?.onItemClick(challenges[position])
                }
            }
        }

        fun bind(challenge: Challenge) {
            binding.tvChallengeTitle.text = challenge.title

            binding.tvChallengeStatus.text = when (challenge.status) {
                "recruiting" -> "모집중"
                "in-progress" -> "진행중"
                "completed" -> "완료"
                else -> challenge.status
            }

            // 참여 인원 데이터 바인딩
            binding.tvParticipantCount.text = "${challenge.participantCount}명"

            Glide.with(itemView.context)
                .load(challenge.imageUrl)
                .placeholder(R.drawable.edittext_background)
                .error(R.drawable.edittext_background)
                .into(binding.ivChallengeImage)
        }
    }
}