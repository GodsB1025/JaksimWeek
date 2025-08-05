package com.team.jaksimweek.adapter

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
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

            val statusTextView = binding.tvChallengeStatus
            val context = itemView.context
            when (challenge.status) {
                "recruiting" -> {
                    statusTextView.text = "모집중"
                    (statusTextView.background as? GradientDrawable)?.setColor(ContextCompat.getColor(context, R.color.status_recruiting))
                }
                "in-progress" -> {
                    statusTextView.text = "진행중"
                    (statusTextView.background as? GradientDrawable)?.setColor(ContextCompat.getColor(context, R.color.status_inprogress))
                }
                "completed" -> {
                    statusTextView.text = "완료"
                    (statusTextView.background as? GradientDrawable)?.setColor(ContextCompat.getColor(context, R.color.status_completed))
                }
                else -> statusTextView.text = challenge.status
            }

            binding.tvParticipantCount.text = "${challenge.participantCount}명"

            Glide.with(itemView.context)
                .load(challenge.imageUrl)
                .placeholder(R.drawable.edittext_background)
                .error(R.drawable.edittext_background)
                .into(binding.ivChallengeImage)
        }
    }
}