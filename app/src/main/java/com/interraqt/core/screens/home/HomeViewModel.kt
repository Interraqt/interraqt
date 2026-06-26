package com.interraqt.core.screens.home

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.DocumentSnapshot

class HomeViewModel : ViewModel() {
    var posts by mutableStateOf<List<FeedPost>>(emptyList())
    var usersMap by mutableStateOf<Map<String, FeedUserProfile>>(emptyMap())
    var lastVisible by mutableStateOf<DocumentSnapshot?>(null)
    var isLoadingMore by mutableStateOf(false)
    var hasMore by mutableStateOf(true)
    var lastFetchTime by mutableLongStateOf(0L) 
}
