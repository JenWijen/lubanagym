package com.duta.lubanagym.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.repository.AuthRepository
import kotlinx.coroutines.launch

class ForgotPasswordViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val authRepository = AuthRepository(firebaseService)

    private val _sendTokenResult = MutableLiveData<Result<String>>()
    val sendTokenResult: LiveData<Result<String>> = _sendTokenResult

    private val _resetPasswordResult = MutableLiveData<Result<String>>()
    val resetPasswordResult: LiveData<Result<String>> = _resetPasswordResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    suspend fun sendPasswordResetToken(email: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val result = authRepository.sendPasswordResetToken(email)
                _sendTokenResult.postValue(result)
            } catch (e: Exception) {
                _sendTokenResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    suspend fun resetPassword(email: String, token: String, newPassword: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val result = authRepository.resetPassword(email, token, newPassword)
                _resetPasswordResult.postValue(result)
            } catch (e: Exception) {
                _resetPasswordResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}