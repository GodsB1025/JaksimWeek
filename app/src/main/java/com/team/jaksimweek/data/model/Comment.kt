package com.team.jaksimweek.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Comment(
    @DocumentId val id: String = "",
    val writerUid: String = "",
    val writerNickname: String? = null,
    val content: String = "",
    val createdAt: Timestamp = Timestamp.now()
)