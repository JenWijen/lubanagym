package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Staff
import com.duta.lubanagym.data.repository.StaffRepository
import kotlinx.coroutines.launch

class StaffManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val staffRepository = StaffRepository(firebaseService)

    private val _staffList = MutableLiveData<Result<List<Staff>>>()
    val staffList: LiveData<Result<List<Staff>>> = _staffList

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun loadStaff() {
        viewModelScope.launch {
            try {
                val result = staffRepository.getAllStaff()
                _staffList.postValue(result)
            } catch (e: Exception) {
                _staffList.postValue(Result.failure(e))
            }
        }
    }

    fun updateStaff(staffId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val result = staffRepository.updateStaff(staffId, updates)
                _updateResult.postValue(result)
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteStaff(staffId: String) {
        viewModelScope.launch {
            try {
                val result = staffRepository.deleteStaff(staffId)
                _deleteResult.postValue(result)
            } catch (e: Exception) {
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}
