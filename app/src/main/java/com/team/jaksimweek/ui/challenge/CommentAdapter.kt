package com.team.jaksimweek.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.team.jaksimweek.data.model.Comment // Comment 모델 import
import com.team.jaksimweek.databinding.ItemCommentBinding

// 생성자에서 받는 데이터 타입을 List<Comment>로 변경합니다.
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

    // 데이터를 업데이트하는 함수
    fun updateData(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    inner class CommentViewHolder(private val binding: ItemCommentBinding) :
        RecyclerView.ViewHolder(binding.root) {

        // bind 함수의 파라미터를 Comment 타입으로 변경합니다.
        fun bind(comment: Comment) {
            binding.commentNickname.text = comment.writerNickname ?: "익명"
            binding.commentContent.text = comment.content
            // 아이콘은 필요에 따라 설정하거나 제거합니다.
            // 예: Glide를 사용하여 작성자의 프로필 이미지를 로드할 수 있습니다.
            // Glide.with(itemView.context).load(comment.writerProfileImageUrl).into(binding.commentIcon)
        }
    }
}