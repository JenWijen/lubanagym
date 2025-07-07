package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.data.repository.EquipmentRepository
import kotlinx.coroutines.launch

class EquipmentManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val equipmentRepository = EquipmentRepository(firebaseService)

    private val _equipmentList = MutableLiveData<Result<List<Equipment>>>()
    val equipmentList: LiveData<Result<List<Equipment>>> = _equipmentList

    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun loadEquipment() {
        viewModelScope.launch {
            try {
                val result = equipmentRepository.getAllEquipment()
                _equipmentList.postValue(result)
            } catch (e: Exception) {
                _equipmentList.postValue(Result.failure(e))
            }
        }
    }

    fun createEquipment(equipment: Equipment) {
        viewModelScope.launch {
            try {
                val result = equipmentRepository.createEquipment(equipment)
                _createResult.postValue(result)
            } catch (e: Exception) {
                _createResult.postValue(Result.failure(e))
            }
        }
    }

    fun updateEquipment(equipmentId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val result = equipmentRepository.updateEquipment(equipmentId, updates)
                _updateResult.postValue(result)
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteEquipment(equipmentId: String) {
        viewModelScope.launch {
            try {
                val result = equipmentRepository.deleteEquipment(equipmentId)
                _deleteResult.postValue(result)
            } catch (e: Exception) {
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}