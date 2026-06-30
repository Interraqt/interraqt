package com.interraqt.core.viewmodels // Update to match your package structure

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class UsernameState {
    IDLE, LOADING, AVAILABLE, UNAVAILABLE, INVALID_FORMAT
}

class EditProfileViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()
    
    private val _usernameState = MutableStateFlow(UsernameState.IDLE)
    val usernameState: StateFlow<UsernameState> = _usernameState.asStateFlow()

    var originalUsername: String = ""
        private set

    private var searchJob: Job? = null
    
    // Allows A-Z, a-z, 0-9, _, and .
    private val usernameRegex = Regex("^[a-zA-Z0-9_.]+$")

    fun setInitialUsername(username: String) {
        originalUsername = username
    }

    fun checkUsernameAvailability(username: String) {
        searchJob?.cancel() // Cancel the previous timer if the user keeps typing
        
        val trimmedUsername = username.trim().lowercase()

        // 1. Check if empty
        if (trimmedUsername.isEmpty()) {
            _usernameState.value = UsernameState.IDLE
            return
        }

        // 2. Check valid format
        if (!usernameRegex.matches(trimmedUsername)) {
            _usernameState.value = UsernameState.INVALID_FORMAT
            return
        }

        // 3. Check if it's the user's current username
        if (trimmedUsername == originalUsername.lowercase()) {
            _usernameState.value = UsernameState.AVAILABLE
            return
        }

        _usernameState.value = UsernameState.LOADING

        // 4. Debounce and check database
        searchJob = viewModelScope.launch {
            delay(500) // Wait 500ms after they stop typing before reading from DB
            try {
                val document = firestore.collection("usernames").document(trimmedUsername).get().await()
                if (document.exists()) {
                    _usernameState.value = UsernameState.UNAVAILABLE
                } else {
                    _usernameState.value = UsernameState.AVAILABLE
                }
            } catch (e: Exception) {
                _usernameState.value = UsernameState.IDLE
            }
        }
    }
}
