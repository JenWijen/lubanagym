package com.duta.lubanagym.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    private var _isInitialized = false
    val isInitialized: Boolean get() = _isInitialized

    fun initializeApp(onComplete: () -> Unit) {
        viewModelScope.launch {
            // Simulate app initialization
            delay(2000) // 2 seconds splash
            _isInitialized = true
            onComplete()
        }
    }
}