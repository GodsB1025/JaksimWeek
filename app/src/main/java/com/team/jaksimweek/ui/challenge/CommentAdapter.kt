package com.team.jaksimweek.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide // Glide import 추가
import com.team.jaksimweek.R // R 클래스 import 추가
import com.team.jaksimweek.data.model.Comment
import com.team.jaksimweek.databinding.ItemCommentBinding
import java.text.SimpleDateFormat
import java.util.Locale

class CommentAdapter(private var comments: List<Comment>) :
    RecyclerView.Adapter<CommentAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val binding =
            ItemCommentBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CommentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        holder.bind(comments[position])
    }

    override fun getItemCount(): Int = comments.size

    fun updateData(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(comment: Comment) {
            binding.commentNickname.text = comment.writerNickname ?: "익명"
            binding.commentContent.text = comment.content

            val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            binding.commentTimestamp.text = sdf.format(comment.createdAt.toDate())

            Glide.with(itemView.context)
                .load(comment.writerProfileImageUrl)
                .placeholder(R.drawable.ic_person)
                .error(R.drawable.ic_person)
                .circleCrop()
                .into(binding.commentIcon)
        }
    }
}