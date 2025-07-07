package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Token
import com.duta.lubanagym.data.repository.TokenRepository
import kotlinx.coroutines.launch

class TokenManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val tokenRepository = TokenRepository(firebaseService)

    private val _tokenList = MutableLiveData<Result<List<Token>>>()
    val tokenList: LiveData<Result<List<Token>>> = _tokenList

    private val _generateResult = MutableLiveData<Result<String>>()
    val generateResult: LiveData<Result<String>> = _generateResult

    fun loadTokens() {
        viewModelScope.launch {
            try {
                val result = tokenRepository.getAllTokens()
                _tokenList.postValue(result)
            } catch (e: Exception) {
                _tokenList.postValue(Result.failure(e))
            }
        }
    }

    fun generateToken(createdBy: String) {
        viewModelScope.launch {
            try {
                val result = tokenRepository.generateToken(createdBy)
                _generateResult.postValue(result)
            } catch (e: Exception) {
                _generateResult.postValue(Result.failure(e))
            }
        }
    }
}