package com.team.jaksimweek.ui.chat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.team.jaksimweek.R
import com.team.jaksimweek.data.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val myUid: String) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messageList = mutableListOf<ChatMessage>()

    companion object {
        private const val VIEW_TYPE_SENT_TEXT = 1
        private const val VIEW_TYPE_RECEIVED_TEXT = 2
        private const val VIEW_TYPE_SENT_IMAGE = 3
        private const val VIEW_TYPE_RECEIVED_IMAGE = 4
    }

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        val isSent = message.senderUid == myUid
        val isText = message.messageType == "text"

        return when {
            isSent && isText -> VIEW_TYPE_SENT_TEXT
            !isSent && isText -> VIEW_TYPE_RECEIVED_TEXT
            isSent && !isText -> VIEW_TYPE_SENT_IMAGE
            else -> VIEW_TYPE_RECEIVED_IMAGE
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT_TEXT -> SentTextHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_chat_text_sent, parent, false)
            )
            VIEW_TYPE_RECEIVED_TEXT -> ReceivedTextHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_chat_text_received, parent, false)
            )
            VIEW_TYPE_SENT_IMAGE -> SentImageHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_chat_image_sent, parent, false)
            )
            else -> ReceivedImageHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_chat_image_received, parent, false)
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        when (holder.itemViewType) {
            VIEW_TYPE_SENT_TEXT -> (holder as SentTextHolder).bind(message)
            VIEW_TYPE_RECEIVED_TEXT -> (holder as ReceivedTextHolder).bind(message)
            VIEW_TYPE_SENT_IMAGE -> (holder as SentImageHolder).bind(message)
            VIEW_TYPE_RECEIVED_IMAGE -> (holder as ReceivedImageHolder).bind(message)
        }
    }

    override fun getItemCount(): Int = messageList.size

    fun addMessage(message: ChatMessage) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    inner class SentTextHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.msgTextView)
        private val timeText: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(message: ChatMessage) {
            messageText.text = message.text
            timeText.text = formatDate(message.timestamp)
        }
    }

    inner class ReceivedTextHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImageView)
        private val nicknameText: TextView = itemView.findViewById(R.id.nicknameTextView)
        private val messageText: TextView = itemView.findViewById(R.id.msgTextView)
        private val timeText: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(message: ChatMessage) {
            nicknameText.text = message.senderNickname
            messageText.text = message.text
            timeText.text = formatDate(message.timestamp)
            Glide.with(itemView.context)
                .load(message.senderProfileImageUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(profileImage)
        }
    }

    inner class SentImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageImage: ImageView = itemView.findViewById(R.id.img_message)
        private val timeText: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(message: ChatMessage) {
            timeText.text = formatDate(message.timestamp)
            Glide.with(itemView.context)
                .load(message.imageUrl)
                .into(messageImage)
        }
    }

    inner class ReceivedImageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImageView)
        private val nicknameText: TextView = itemView.findViewById(R.id.nicknameTextView)
        private val messageImage: ImageView = itemView.findViewById(R.id.img_message)
        private val timeText: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(message: ChatMessage) {
            nicknameText.text = message.senderNickname
            timeText.text = formatDate(message.timestamp)
            Glide.with(itemView.context)
                .load(message.senderProfileImageUrl)
                .placeholder(R.drawable.ic_person)
                .circleCrop()
                .into(profileImage)

            Glide.with(itemView.context)
                .load(message.imageUrl)
                .into(messageImage)
        }
    }
}