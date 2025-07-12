package com.duta.lubanagym.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.repository.AuthRepository
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val authRepository = AuthRepository(firebaseService)

    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> = _registerResult

    // NEW: Google Sign-In result
    private val _googleSignInResult = MutableLiveData<Result<User>>()
    val googleSignInResult: LiveData<Result<User>> = _googleSignInResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    // UPDATED: Register tanpa token
    suspend fun register(email: String, password: String, username: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val result = authRepository.register(email, password, username)
                _registerResult.postValue(result)

                // Auto-login after successful registration
                result.onSuccess {
                    // Login otomatis setelah registrasi berhasil
                    val loginResult = authRepository.login(email, password)
                    loginResult.onSuccess { user ->
                        // Save login state
                        // Note: PreferenceHelper needs to be passed from Activity
                        // This is just for demo - in real app, handle this in Activity
                    }
                }
            } catch (e: Exception) {
                _registerResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    // NEW: Google Sign-In method
    suspend fun signInWithGoogle(idToken: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val result = authRepository.signInWithGoogle(idToken)
                _googleSignInResult.postValue(result)
            } catch (e: Exception) {
                _googleSignInResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}