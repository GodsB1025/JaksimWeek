package com.team.jaksimweek.ui.challenge

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.team.jaksimweek.databinding.ItemCommentBinding

class CommentAdapter(val context: Context, val dataset: ArrayList<CommentVO>) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(val binding: ItemCommentBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: CommentVO) {
            binding.commentNickname.text = item.nickname
            binding.commentContent.text = item.content
            binding.commentIcon.setImageResource(item.icon)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return dataset.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(dataset[position])
    }

}
