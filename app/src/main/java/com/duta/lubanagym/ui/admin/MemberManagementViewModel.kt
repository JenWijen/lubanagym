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

    private val _memberList = MutableLiveData<Result<List<Member>>>()
    val memberList: LiveData<Result<List<Member>>> = _memberList

    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    fun loadMembers() {
        viewModelScope.launch {
            try {
                val result = memberRepository.getAllMembers()
                _memberList.postValue(result)
            } catch (e: Exception) {
                _memberList.postValue(Result.failure(e))
            }
        }
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