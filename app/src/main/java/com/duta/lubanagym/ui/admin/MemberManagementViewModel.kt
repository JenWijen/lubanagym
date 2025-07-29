package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.data.repository.MemberRepository
import kotlinx.coroutines.launch

class MemberManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val memberRepository = MemberRepository(firebaseService)

    enum class SortType {
        NEWEST_FIRST,
        OLDEST_FIRST,
        NAME_A_Z,
        NAME_Z_A,
        EXPIRY_SOON,
        EXPIRY_LATEST
    }

    private val _memberList = MutableLiveData<Result<List<Member>>>()
    val memberList: LiveData<Result<List<Member>>> = _memberList

    private val _filteredMemberList = MutableLiveData<Result<List<Member>>>()
    val filteredMemberList: LiveData<Result<List<Member>>> = _filteredMemberList

    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private var allMembers = listOf<Member>()
    private var currentSearchQuery = ""
    private var currentMembershipFilter: String? = null
    private var currentStatusFilter: Boolean? = null
    private var currentSortType = SortType.NEWEST_FIRST

    fun loadMembers() {
        viewModelScope.launch {
            try {
                val result = memberRepository.getAllMembers()
                result.onSuccess { members ->
                    allMembers = members
                    applyFiltersAndSort()
                }
                _memberList.postValue(result)
            } catch (e: Exception) {
                _memberList.postValue(Result.failure(e))
                _filteredMemberList.postValue(Result.failure(e))
            }
        }
    }

    fun searchMembers(query: String) {
        currentSearchQuery = query
        applyFiltersAndSort()
    }

    fun filterByMembership(membership: String?) {
        currentMembershipFilter = membership
        applyFiltersAndSort()
    }

    fun filterByStatus(status: Boolean?) {
        currentStatusFilter = status
        applyFiltersAndSort()
    }

    fun sortMembers(sortType: SortType) {
        currentSortType = sortType
        applyFiltersAndSort()
    }

    fun resetFilters() {
        currentSearchQuery = ""
        currentMembershipFilter = null
        currentStatusFilter = null
        currentSortType = SortType.NEWEST_FIRST
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        var filteredMembers = allMembers

        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            filteredMembers = filteredMembers.filter { member ->
                member.name.contains(currentSearchQuery, ignoreCase = true) ||
                        member.phone.contains(currentSearchQuery, ignoreCase = true) ||
                        member.id.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Apply membership filter
        currentMembershipFilter?.let { membership ->
            filteredMembers = filteredMembers.filter { member ->
                member.membershipType.equals(membership, ignoreCase = true)
            }
        }

        // Apply status filter
        currentStatusFilter?.let { status ->
            filteredMembers = filteredMembers.filter { member ->
                member.isActive == status
            }
        }

        // Apply sorting
        filteredMembers = when (currentSortType) {
            SortType.NEWEST_FIRST -> filteredMembers.sortedByDescending { it.joinDate }
            SortType.OLDEST_FIRST -> filteredMembers.sortedBy { it.joinDate }
            SortType.NAME_A_Z -> filteredMembers.sortedBy { it.name.lowercase() }
            SortType.NAME_Z_A -> filteredMembers.sortedByDescending { it.name.lowercase() }
            SortType.EXPIRY_SOON -> filteredMembers.sortedBy { it.expiryDate }
            SortType.EXPIRY_LATEST -> filteredMembers.sortedByDescending { it.expiryDate }
        }

        _filteredMemberList.postValue(Result.success(filteredMembers))
    }

    fun createMember(member: Member) {
        viewModelScope.launch {
            try {
                val result = memberRepository.createMember(member)
                _createResult.postValue(result)
            } catch (e: Exception) {
                _createResult.postValue(Result.failure(e))
            }
        }
    }

    fun updateMember(memberId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val result = memberRepository.updateMember(memberId, updates)
                _updateResult.postValue(result)
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteMember(memberId: String) {
        viewModelScope.launch {
            try {
                val result = memberRepository.deleteMember(memberId)
                _deleteResult.postValue(result)
            } catch (e: Exception) {
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}