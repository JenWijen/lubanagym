package com.duta.lubanagym.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.data.repository.EquipmentRepository
import kotlinx.coroutines.launch

class EquipmentViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val equipmentRepository = EquipmentRepository(firebaseService)

    private val _equipmentList = MutableLiveData<Result<List<Equipment>>>()
    val equipmentList: LiveData<Result<List<Equipment>>> = _equipmentList

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
}