package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.data.model.Staff
import com.duta.lubanagym.data.model.Trainer
import com.duta.lubanagym.data.repository.UserRepository
import com.duta.lubanagym.data.repository.MemberRepository
import com.duta.lubanagym.data.repository.StaffRepository
import com.duta.lubanagym.data.repository.TrainerRepository
import com.duta.lubanagym.utils.Constants
import kotlinx.coroutines.launch

class UserManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val userRepository = UserRepository(firebaseService)
    private val memberRepository = MemberRepository(firebaseService)
    private val staffRepository = StaffRepository(firebaseService)
    private val trainerRepository = TrainerRepository(firebaseService)

    private val _userList = MutableLiveData<Result<List<User>>>()
    val userList: LiveData<Result<List<User>>> = _userList

    private val _updateResult = MutableLiveData<Result<String>>()
    val updateResult: LiveData<Result<String>> = _updateResult

    // NEW: Delete result LiveData
    private val _deleteResult = MutableLiveData<Result<String>>()
    val deleteResult: LiveData<Result<String>> = _deleteResult

    fun loadUsers() {
        viewModelScope.launch {
            try {
                val result = userRepository.getAllUsers()
                _userList.postValue(result)
            } catch (e: Exception) {
                _userList.postValue(Result.failure(e))
            }
        }
    }

    // NEW: Delete user function
    fun deleteUser(userId: String, userRole: String) {
        viewModelScope.launch {
            try {
                // 1. Delete role-specific data first
                val cleanupResult = cleanupUserRoleData(userId, userRole)

                if (cleanupResult) {
                    // 2. Delete user from users collection
                    val deleteUserResult = userRepository.deleteUser(userId)

                    deleteUserResult.onSuccess {
                        _deleteResult.postValue(Result.success("✅ User berhasil dihapus dari sistem"))
                    }.onFailure { error ->
                        _deleteResult.postValue(Result.failure(Exception("Gagal menghapus user: ${error.message}")))
                    }
                } else {
                    _deleteResult.postValue(Result.failure(Exception("Gagal membersihkan data role user")))
                }

            } catch (e: Exception) {
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }

    private suspend fun cleanupUserRoleData(userId: String, userRole: String): Boolean {
        return try {
            when (userRole) {
                Constants.ROLE_MEMBER -> {
                    val result = memberRepository.deleteMemberByUserId(userId)
                    result.isSuccess
                }
                Constants.ROLE_STAFF -> {
                    val result = staffRepository.deleteStaffByUserId(userId)
                    result.isSuccess
                }
                Constants.ROLE_TRAINER -> {
                    val result = trainerRepository.deleteTrainerByUserId(userId)
                    result.isSuccess
                }
                Constants.ROLE_ADMIN -> {
                    // Admin tidak punya collection terpisah
                    true
                }
                else -> true // Unknown role, proceed
            }
        } catch (e: Exception) {
            false
        }
    }

    fun updateUserRole(userId: String, oldRole: String, newRole: String) {
        viewModelScope.launch {
            try {
                if (oldRole == newRole) {
                    _updateResult.postValue(Result.success("ℹ️ Role sudah sama, tidak ada perubahan"))
                    return@launch
                }

                val updateResult = userRepository.updateUserRole(userId, newRole)

                if (updateResult.isSuccess) {
                    val userResult = userRepository.getUserById(userId)

                    userResult.onSuccess { user ->
                        cleanupOldRoleData(userId, oldRole) { cleanupSuccess ->
                            if (cleanupSuccess) {
                                when (newRole) {
                                    Constants.ROLE_MEMBER -> createMemberProfile(user, oldRole)
                                    Constants.ROLE_STAFF -> createStaffProfile(user, oldRole)
                                    Constants.ROLE_TRAINER -> createTrainerProfile(user, oldRole)
                                    Constants.ROLE_ADMIN -> {
                                        val cleanupMsg = if (oldRole != Constants.ROLE_ADMIN) {
                                            " & data $oldRole dihapus"
                                        } else ""
                                        _updateResult.postValue(Result.success("✅ Role berhasil diupdate ke Admin$cleanupMsg"))
                                    }
                                }
                            } else {
                                _updateResult.postValue(Result.failure(Exception("Gagal membersihkan data role lama")))
                            }
                        }
                    }.onFailure { error ->
                        _updateResult.postValue(Result.failure(error))
                    }
                } else {
                    _updateResult.postValue(updateResult.map { "✅ Role berhasil diupdate" })
                }

            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    private fun cleanupOldRoleData(userId: String, oldRole: String, callback: (Boolean) -> Unit) {
        viewModelScope.launch {
            try {
                when (oldRole) {
                    Constants.ROLE_MEMBER -> {
                        memberRepository.deleteMemberByUserId(userId).onSuccess {
                            callback(true)
                        }.onFailure {
                            callback(false)
                        }
                    }

                    Constants.ROLE_STAFF -> {
                        staffRepository.deleteStaffByUserId(userId).onSuccess {
                            callback(true)
                        }.onFailure {
                            callback(false)
                        }
                    }

                    Constants.ROLE_TRAINER -> {
                        trainerRepository.deleteTrainerByUserId(userId).onSuccess {
                            callback(true)
                        }.onFailure {
                            callback(false)
                        }
                    }

                    Constants.ROLE_ADMIN -> {
                        callback(true)
                    }

                    else -> {
                        callback(true)
                    }
                }
            } catch (e: Exception) {
                callback(false)
            }
        }
    }

    private fun createMemberProfile(user: User, oldRole: String) {
        viewModelScope.launch {
            try {
                val newMember = Member(
                    id = "",
                    userId = user.id,
                    name = user.username,
                    phone = "",
                    membershipType = Constants.MEMBERSHIP_BASIC,
                    joinDate = System.currentTimeMillis(),
                    expiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                    isActive = true,
                    profileImageUrl = "",
                    qrCode = generateQRCode(user.id)
                )

                memberRepository.createMember(newMember).onSuccess {
                    val cleanupMsg = if (oldRole != Constants.ROLE_MEMBER) " & data $oldRole dihapus" else ""
                    _updateResult.postValue(Result.success("✅ Role diupdate ke Member, profil member dibuat$cleanupMsg"))
                }.onFailure { error ->
                    _updateResult.postValue(Result.failure(Exception("Role diupdate tapi gagal membuat profil member: ${error.message}")))
                }
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(Exception("Error creating member profile: ${e.message}")))
            }
        }
    }

    private fun createStaffProfile(user: User, oldRole: String) {
        viewModelScope.launch {
            try {
                val newStaff = Staff(
                    id = "",
                    userId = user.id,
                    name = user.username,
                    phone = "",
                    position = "Staff",
                    joinDate = System.currentTimeMillis(),
                    isActive = true,
                    profileImageUrl = "",
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis()
                )

                staffRepository.createStaff(newStaff).onSuccess {
                    val cleanupMsg = if (oldRole != Constants.ROLE_STAFF) " & data $oldRole dihapus" else ""
                    _updateResult.postValue(Result.success("✅ Role diupdate ke Staff, profil staff dibuat$cleanupMsg"))
                }.onFailure { error ->
                    _updateResult.postValue(Result.failure(Exception("Role diupdate tapi gagal membuat profil staff: ${error.message}")))
                }
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(Exception("Error creating staff profile: ${e.message}")))
            }
        }
    }

    private fun createTrainerProfile(user: User, oldRole: String) {
        viewModelScope.launch {
            try {
                val newTrainer = Trainer(
                    id = "",
                    userId = user.id,
                    name = user.username,
                    phone = "",
                    specialization = "General Fitness",
                    experience = "Beginner",
                    bio = "Trainer baru dari sistem",
                    profileImageUrl = "",
                    isActive = true,
                    createdAt = System.currentTimeMillis()
                )

                trainerRepository.createTrainer(newTrainer).onSuccess {
                    val cleanupMsg = if (oldRole != Constants.ROLE_TRAINER) " & data $oldRole dihapus" else ""
                    _updateResult.postValue(Result.success("✅ Role diupdate ke Trainer, profil trainer dibuat$cleanupMsg"))
                }.onFailure { error ->
                    _updateResult.postValue(Result.failure(Exception("Role diupdate tapi gagal membuat profil trainer: ${error.message}")))
                }
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(Exception("Error creating trainer profile: ${e.message}")))
            }
        }
    }

    private fun generateQRCode(userId: String): String {
        return "LUBANA_MEMBER_${userId.take(8).uppercase()}"
    }
}