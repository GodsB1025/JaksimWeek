package com.team.jaksimweek.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class Challenge(
    @DocumentId val id: String = "",
    val creatorUid: String = "",
    val creatorNickname: String? = null,
    val title: String = "",
    val description: String = "",
    val imageUrl: String? = null,
    val status: String = "recruiting", // "recruiting", "in-progress", "completed"
    val location: Location? = null,
    val participantCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now()
)

data class Location(
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val addressName: String = ""
)