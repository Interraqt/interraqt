package com.interraqt.core.screens.home

data class FeedPost(
    val postId: String = "",
    val userId: String = "",
    val caption: String = "",
    val mediaUrls: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

data class FeedUserProfile(
    val username: String = "Unknown",
    val profileImageUrl: String = ""
)

data class PostComment(
    val commentId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
