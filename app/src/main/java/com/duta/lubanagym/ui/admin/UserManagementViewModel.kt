package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.data.model.Staff
import com.duta.lubanagym.data.repository.UserRepository
import com.duta.lubanagym.data.repository.MemberRepository
import com.duta.lubanagym.data.repository.StaffRepository
// HAPUS import TrainerRepository
import com.duta.lubanagym.utils.Constants
import kotlinx.coroutines.launch

class UserManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val userRepository = UserRepository(firebaseService)
    private val staffRepository = StaffRepository(firebaseService)
    private val memberRepository = MemberRepository(firebaseService)
    // HAPUS trainerRepository

    private val _userList = MutableLiveData<Result<List<User>>>()
    val userList: LiveData<Result<List<User>>> = _userList

    private val _updateResult = MutableLiveData<Result<String>>()
    val updateResult: LiveData<Result<String>> = _updateResult

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

    fun deleteUser(userId: String, userRole: String) {
        viewModelScope.launch {
            try {
                val cleanupResult = cleanupUserRoleData(userId, userRole)

                if (cleanupResult) {
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
                // HAPUS case Constants.ROLE_TRAINER
                Constants.ROLE_ADMIN, Constants.ROLE_GUEST -> true
                else -> true
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
                                    Constants.ROLE_MEMBER -> createMemberProfileWithUserData(user, oldRole)
                                    Constants.ROLE_STAFF -> createStaffProfileWithUserData(user, oldRole)
                                    // HAPUS case Constants.ROLE_TRAINER
                                    Constants.ROLE_ADMIN -> {
                                        val cleanupMsg = if (oldRole != Constants.ROLE_ADMIN) {
                                            " & data $oldRole dihapus"
                                        } else ""
                                        _updateResult.postValue(Result.success("✅ Role berhasil diupdate ke Admin$cleanupMsg"))
                                    }
                                    Constants.ROLE_GUEST -> {
                                        val cleanupMsg = if (oldRole != Constants.ROLE_GUEST) {
                                            " & data $oldRole dihapus"
                                        } else ""
                                        _updateResult.postValue(Result.success("✅ Role berhasil diupdate ke Guest$cleanupMsg"))
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
                    _updateResult.postValue(Result.success("✅ Role berhasil diupdate"))
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

                    // HAPUS case Constants.ROLE_TRAINER

                    Constants.ROLE_ADMIN, Constants.ROLE_GUEST -> {
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

    private fun createMemberProfileWithUserData(user: User, oldRole: String) {
        viewModelScope.launch {
            try {
                val newMember = Member(
                    id = "",
                    userId = user.id,
                    name = if (user.fullName.isNotEmpty()) user.fullName else user.username,
                    phone = user.phone,
                    membershipType = Constants.MEMBERSHIP_BASIC,
                    joinDate = System.currentTimeMillis(),
                    expiryDate = System.currentTimeMillis() + (365L * 24 * 60 * 60 * 1000),
                    isActive = true,
                    profileImageUrl = user.profileImageUrl,
                    qrCode = generateQRCode(user.id)
                )

                memberRepository.createMember(newMember).onSuccess {
                    val cleanupMsg = if (oldRole != Constants.ROLE_MEMBER) " & data $oldRole dihapus" else ""
                    _updateResult.postValue(Result.success("✅ Role diupdate ke Member, profil member dibuat dengan data lengkap$cleanupMsg"))
                }.onFailure { error ->
                    _updateResult.postValue(Result.failure(Exception("Role diupdate tapi gagal membuat profil member: ${error.message}")))
                }
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(Exception("Error creating member profile: ${e.message}")))
            }
        }
    }

    private fun createStaffProfileWithUserData(user: User, oldRole: String) {
        viewModelScope.launch {
            try {
                val newStaff = Staff(
                    id = "",
                    userId = user.id,
                    name = if (user.fullName.isNotEmpty()) user.fullName else user.username,
                    phone = user.phone,
                    position = "Staff",
                    joinDate = System.currentTimeMillis(),
                    isActive = true,
                    profileImageUrl = user.profileImageUrl,
                    createdAt = System.currentTimeMillis(),
                    updatedAt = System.currentTimeMillis(),

                    // Extended profile data
                    address = user.address,
                    emergencyContact = user.emergencyContact,
                    emergencyPhone = user.emergencyPhone,
                    dateOfBirth = user.dateOfBirth,
                    gender = user.gender
                )

                staffRepository.createStaff(newStaff).onSuccess {
                    val cleanupMsg = if (oldRole != Constants.ROLE_STAFF) " & data $oldRole dihapus" else ""
                    _updateResult.postValue(Result.success("✅ Role diupdate ke Staff, profil staff dibuat dengan data lengkap$cleanupMsg"))
                }.onFailure { error ->
                    _updateResult.postValue(Result.failure(Exception("Role diupdate tapi gagal membuat profil staff: ${error.message}")))
                }
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(Exception("Error creating staff profile: ${e.message}")))
            }
        }
    }

    // HAPUS createTrainerProfileWithUserData method

    private fun generateQRCode(userId: String): String {
        return "LUBANA_MEMBER_${userId.take(8).uppercase()}"
    }
}