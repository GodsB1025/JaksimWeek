package com.team.jaksimweek.data.model

import com.google.firebase.Timestamp

data class User(
    val uid: String = "",
    val email: String? = null,
    val nickname: String? = null,
    val profileImageUrl: String? = null,
    val bio: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val likedChallengeIds: List<String> = emptyList(),
    val bookmarkedChallengeIds: List<String> = emptyList()
)