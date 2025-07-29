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

    enum class SortType {
        NEWEST_FIRST,
        OLDEST_FIRST,
        NAME_A_Z,
        NAME_Z_A
    }

    private val _staffList = MutableLiveData<Result<List<Staff>>>()
    val staffList: LiveData<Result<List<Staff>>> = _staffList

    private val _filteredStaffList = MutableLiveData<Result<List<Staff>>>()
    val filteredStaffList: LiveData<Result<List<Staff>>> = _filteredStaffList

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private var allStaff = listOf<Staff>()
    private var currentSearchQuery = ""
    private var currentPositionFilter: String? = null
    private var currentStatusFilter: Boolean? = null
    private var currentSortType = SortType.NEWEST_FIRST

    fun loadStaff() {
        viewModelScope.launch {
            try {
                val result = staffRepository.getAllStaff()
                result.onSuccess { staff ->
                    allStaff = staff
                    applyFiltersAndSort()
                }
                _staffList.postValue(result)
            } catch (e: Exception) {
                _staffList.postValue(Result.failure(e))
                _filteredStaffList.postValue(Result.failure(e))
            }
        }
    }

    fun searchStaff(query: String) {
        currentSearchQuery = query
        applyFiltersAndSort()
    }

    fun filterByPosition(position: String?) {
        currentPositionFilter = position
        applyFiltersAndSort()
    }

    fun filterByStatus(isActive: Boolean?) {
        currentStatusFilter = isActive
        applyFiltersAndSort()
    }

    fun sortStaff(sortType: SortType) {
        currentSortType = sortType
        applyFiltersAndSort()
    }

    fun resetFilters() {
        currentSearchQuery = ""
        currentPositionFilter = null
        currentStatusFilter = null
        currentSortType = SortType.NEWEST_FIRST
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        var filteredStaff = allStaff

        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            filteredStaff = filteredStaff.filter { staff ->
                staff.name.contains(currentSearchQuery, ignoreCase = true) ||
                        staff.phone.contains(currentSearchQuery, ignoreCase = true) ||
                        staff.position.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Apply position filter
        currentPositionFilter?.let { position ->
            filteredStaff = filteredStaff.filter { staff ->
                staff.position.equals(position, ignoreCase = true)
            }
        }

        // Apply status filter
        currentStatusFilter?.let { isActive ->
            filteredStaff = filteredStaff.filter { staff ->
                staff.isActive == isActive
            }
        }

        // Apply sorting
        filteredStaff = when (currentSortType) {
            SortType.NEWEST_FIRST -> filteredStaff.sortedByDescending { it.joinDate }
            SortType.OLDEST_FIRST -> filteredStaff.sortedBy { it.joinDate }
            SortType.NAME_A_Z -> filteredStaff.sortedBy { it.name }
            SortType.NAME_Z_A -> filteredStaff.sortedByDescending { it.name }
        }

        _filteredStaffList.postValue(Result.success(filteredStaff))
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