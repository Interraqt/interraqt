package com.interraqt.core.screens.home

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class HomeViewModel : ViewModel() {
    var posts by mutableStateOf<List<FeedPost>>(emptyList())
    var usersMap by mutableStateOf<Map<String, FeedUserProfile>>(emptyMap())
    var lastVisible by mutableStateOf<DocumentSnapshot?>(null)
    var isLoadingMore by mutableStateOf(false)
    var hasMore by mutableStateOf(true)
    var lastFetchTime by mutableLongStateOf(0L)

        private val firestore = FirebaseFirestore.getInstance()
 
        // 👇 Adding "get()" forces the app to ask Firebase for the REAL user 
    // every single time the screen draws, instead of memorizing the first one!
    val currentUserId: String
        get() = FirebaseAuth.getInstance().currentUser?.uid ?: ""


    init {
        loadPosts(isRefresh = true) // 🚨 FIX: Forces a fresh pull instantly on cold boot (app swipe-kill)
    }

    // 🚨 Moved from HomeScreen: Time logic belongs in the ViewModel

    fun getShortTime(time: Long): String {
        if (time == 0L) return ""
        val diff = System.currentTimeMillis() - time
        val minutes = diff / (1000 * 60)
        val hours = minutes / 60
        val days = hours / 24
        return when {
            days > 0 -> "${days}d ago"
            hours > 0 -> "${hours}h ago"
            minutes > 0 -> "${minutes}m ago"
            else -> "Just now"
        }
    }

    // 🚨 Moved from HomeScreen: Database logic belongs in the ViewModel
    fun loadPosts(isRefresh: Boolean = false, onRefreshComplete: (() -> Unit)? = null) {
        if (isLoadingMore || (!hasMore && !isRefresh)) {
            onRefreshComplete?.invoke()
            return
        }
        isLoadingMore = true
        
        if (isRefresh) {
            lastVisible = null
            hasMore = true
        }
        
        var query = firestore.collection("posts")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(24)
            
        if (!isRefresh && lastVisible != null) {
            query = query.startAfter(lastVisible!!)
        }

        query.get().addOnSuccessListener { snapshot ->
            if (isRefresh) lastFetchTime = System.currentTimeMillis()
            
            if (snapshot.isEmpty) {
                hasMore = false
            } else {
                lastVisible = snapshot.documents.last()
                val fetchedPosts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                val randomizedBatch = fetchedPosts.shuffled() 
                
                posts = if (isRefresh) randomizedBatch else posts + randomizedBatch
                
                val missingUsers = randomizedBatch.map { it.userId }.distinct().filter { !usersMap.containsKey(it) }
                missingUsers.forEach { uid ->
                    firestore.collection("users").document(uid).get().addOnSuccessListener { userDoc ->
                        val username = userDoc.getString("username") ?: "Unknown"
                        val profileImageUrl = userDoc.getString("profileImageUrl") ?: ""
                        usersMap = usersMap + (uid to FeedUserProfile(username, profileImageUrl))
                    }
                }
            }
            isLoadingMore = false
            onRefreshComplete?.invoke()
        }.addOnFailureListener {
            isLoadingMore = false
            onRefreshComplete?.invoke()
        }
    }

    fun checkForNewPosts() {
        if (posts.isEmpty()) return
        val latestTimestamp = posts.maxOfOrNull { it.timestamp } ?: return
        
        firestore.collection("posts")
            .whereGreaterThan("timestamp", latestTimestamp)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                if (!snapshot.isEmpty) {
                    val newPosts = snapshot.documents.mapNotNull { it.toObject(FeedPost::class.java) }
                    posts = newPosts + posts 
                }
            }
    }

    fun deletePost(postId: String) {
        firestore.collection("posts").document(postId).delete()
        posts = posts.filter { it.postId != postId }
    }
    
    fun incrementCommentCount(postId: String) {
        posts = posts.map { if (it.postId == postId) it.copy(commentsCount = it.commentsCount + 1) else it }
    }
}
