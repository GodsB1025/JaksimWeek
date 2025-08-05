package com.team.jaksimweek.ui.chat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.team.jaksimweek.R
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(private val context: Context, private val myUid: String) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messageList = mutableListOf<ChatMessage>()

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList.getOrNull(position)?.senderUid == myUid) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_SENT) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_text_sent, parent, false)
            SentMessageHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_text_received, parent, false)
            ReceivedMessageHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList.getOrNull(position) ?: return
        if (holder.itemViewType == VIEW_TYPE_SENT) {
            (holder as SentMessageHolder).bind(message)
        } else {
            (holder as ReceivedMessageHolder).bind(message)
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    fun addMessage(message: ChatMessage) {
        messageList.add(message)
        notifyItemInserted(messageList.size - 1)
    }

    inner class SentMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.msgTextView)
        private val timeText: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(message: ChatMessage) {
            messageText.text = message.text
            timeText.text = formatDate(message.timestamp)
        }
    }

    inner class ReceivedMessageHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nicknameText: TextView = itemView.findViewById(R.id.nicknameTextView)
        private val messageText: TextView = itemView.findViewById(R.id.msgTextView)
        private val timeText: TextView = itemView.findViewById(R.id.timeTextView)

        fun bind(message: ChatMessage) {
            nicknameText.text = message.senderNickname
            messageText.text = message.text
            timeText.text = formatDate(message.timestamp)
        }
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val date = Date(timestamp)
        return sdf.format(date)
    }
}