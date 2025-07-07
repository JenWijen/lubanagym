package com.duta.lubanagym.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.repository.AuthRepository
import kotlinx.coroutines.launch

class RegisterViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val authRepository = AuthRepository(firebaseService)

    private val _registerResult = MutableLiveData<Result<String>>()
    val registerResult: LiveData<Result<String>> = _registerResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    suspend fun register(email: String, password: String, username: String, token: String) {
        _isLoading.postValue(true)
        viewModelScope.launch {
            try {
                val result = authRepository.register(email, password, username, token)
                _registerResult.postValue(result)
            } catch (e: Exception) {
                _registerResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}