package com.interraqt.core.screens.home

import androidx.annotation.Keep // 🚨 Added the required import

@Keep // 🚨 Protects FeedPost from crashing in Release build
data class FeedPost(
    val postId: String = "",
    val userId: String = "",
    val caption: String = "",
    val mediaUrls: List<String> = emptyList(),
    val timestamp: Long = 0L,
    val likesCount: Int = 0,
    val commentsCount: Int = 0
)

@Keep // 🚨 Protects FeedUserProfile from crashing in Release build
data class FeedUserProfile(
    val username: String = "Unknown",
    val profileImageUrl: String = ""
)

@Keep // 🚨 Protects PostComment from crashing in Release build
data class PostComment(
    val commentId: String = "",
    val userId: String = "",
    val text: String = "",
    val timestamp: Long = 0L
)
