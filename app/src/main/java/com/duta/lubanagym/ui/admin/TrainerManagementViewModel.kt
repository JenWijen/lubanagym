package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Trainer
import com.duta.lubanagym.data.repository.TrainerRepository
import kotlinx.coroutines.launch

class TrainerManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val trainerRepository = TrainerRepository(firebaseService)

    private val _trainerList = MutableLiveData<Result<List<Trainer>>>()
    val trainerList: LiveData<Result<List<Trainer>>> = _trainerList

    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun loadTrainers() {
        viewModelScope.launch {
            try {
                val result = trainerRepository.getAllTrainers()
                _trainerList.postValue(result)
            } catch (e: Exception) {
                _trainerList.postValue(Result.failure(e))
            }
        }
    }

    fun createTrainer(trainer: Trainer) {
        viewModelScope.launch {
            try {
                val result = trainerRepository.createTrainer(trainer)
                _createResult.postValue(result)
            } catch (e: Exception) {
                _createResult.postValue(Result.failure(e))
            }
        }
    }

    fun updateTrainer(trainerId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val result = trainerRepository.updateTrainer(trainerId, updates)
                _updateResult.postValue(result)
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteTrainer(trainerId: String) {
        viewModelScope.launch {
            try {
                val result = trainerRepository.deleteTrainer(trainerId)
                _deleteResult.postValue(result)
            } catch (e: Exception) {
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}