package com.duta.lubanagym.ui.member

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.MemberRegistration
import com.duta.lubanagym.data.repository.MemberRegistrationRepository
import com.duta.lubanagym.data.repository.UserRepository
import com.duta.lubanagym.utils.Constants
import kotlinx.coroutines.launch
import java.util.*

class RegisterMemberViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val registrationRepository = MemberRegistrationRepository(firebaseService)
    private val userRepository = UserRepository(firebaseService)

    private val _registrationResult = MutableLiveData<Result<String>>()
    val registrationResult: LiveData<Result<String>> = _registrationResult

    private val _existingRegistration = MutableLiveData<Result<MemberRegistration?>>()
    val existingRegistration: LiveData<Result<MemberRegistration?>> = _existingRegistration

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun checkExistingRegistration(userId: String) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val result = registrationRepository.getRegistrationByUserId(userId)
                _existingRegistration.postValue(result)
            } catch (e: Exception) {
                _existingRegistration.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun registerMember(
        userId: String,
        membershipType: String,
        duration: Int,
        price: Int
    ) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                // Get user data first
                val userResult = userRepository.getUserById(userId)
                if (userResult.isFailure) {
                    _registrationResult.postValue(Result.failure(Exception("User tidak ditemukan")))
                    return@launch
                }

                val user = userResult.getOrNull()!!

                // Generate QR Code
                val qrCode = "LUBANA_REG_${userId.take(8).uppercase()}_${System.currentTimeMillis()}"

                // Calculate expiry date (5 days from now)
                val expiryDate = System.currentTimeMillis() + (5 * 24 * 60 * 60 * 1000L)

                val registration = MemberRegistration(
                    userId = userId,
                    userName = user.username,
                    userEmail = user.email,
                    userPhone = user.phone,
                    membershipType = membershipType,
                    duration = duration,
                    price = price,
                    registrationDate = System.currentTimeMillis(),
                    expiryDate = expiryDate,
                    status = "pending",
                    qrCode = qrCode,
                    isActive = false,

                    // Extended user data for member creation later
                    userFullName = user.fullName,
                    userAddress = user.address,
                    userDateOfBirth = user.dateOfBirth,
                    userGender = user.gender,
                    userEmergencyContact = user.emergencyContact,
                    userEmergencyPhone = user.emergencyPhone,
                    userBloodType = user.bloodType,
                    userAllergies = user.allergies
                )

                val result = registrationRepository.createRegistration(registration)
                result.onSuccess {
                    _registrationResult.postValue(Result.success(qrCode))
                }.onFailure { error ->
                    _registrationResult.postValue(Result.failure(error))
                }

            } catch (e: Exception) {
                _registrationResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }
}