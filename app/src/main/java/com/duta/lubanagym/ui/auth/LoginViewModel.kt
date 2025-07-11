package com.duta.lubanagym.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.repository.AuthRepository
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val authRepository = AuthRepository(firebaseService)

    private val _loginResult = MutableLiveData<Result<User>>()
    val loginResult: LiveData<Result<User>> = _loginResult

    // NEW: Google Sign-In result
    private val _googleSignInResult = MutableLiveData<Result<User>>()
    val googleSignInResult: LiveData<Result<User>> = _googleSignInResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    suspend fun login(email: String, password: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val result = authRepository.login(email, password)
                _loginResult.postValue(result)
            } catch (e: Exception) {
                _loginResult.postValue(Result.failure(e))
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