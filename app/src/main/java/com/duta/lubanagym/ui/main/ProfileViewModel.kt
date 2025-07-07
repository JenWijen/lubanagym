package com.duta.lubanagym.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.data.repository.UserRepository
import com.duta.lubanagym.data.repository.MemberRepository
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val userRepository = UserRepository(firebaseService)
    private val memberRepository = MemberRepository(firebaseService)

    // FIXED: Explicit nullable type untuk clarity
    private val _userProfile = MutableLiveData<Result<User?>>()
    val userProfile: LiveData<Result<User?>> = _userProfile

    private val _memberProfile = MutableLiveData<Result<Member?>>()
    val memberProfile: LiveData<Result<Member?>> = _memberProfile

    private val _updateResult = MutableLiveData<Result<String>>()
    val updateResult: LiveData<Result<String>> = _updateResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun loadUserProfile(userId: String) {
        if (userId.isEmpty()) {
            _userProfile.postValue(Result.failure(Exception("User ID kosong")))
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val result = userRepository.getUserById(userId)
                // Convert Result<User> to Result<User?>
                val nullableResult = result.fold(
                    onSuccess = { user -> Result.success(user) },
                    onFailure = { error ->
                        // Jika user tidak ditemukan, return null instead of error
                        if (error.message?.contains("tidak ditemukan") == true) {
                            Result.success(null)
                        } else {
                            Result.failure(error)
                        }
                    }
                )
                _userProfile.postValue(nullableResult)
            } catch (e: Exception) {
                _userProfile.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    fun loadMemberProfile(userId: String) {
        if (userId.isEmpty()) {
            _memberProfile.postValue(Result.success(null))
            return
        }

        viewModelScope.launch {
            try {
                val result = memberRepository.getMemberByUserId(userId)
                _memberProfile.postValue(result)
            } catch (e: Exception) {
                _memberProfile.postValue(Result.failure(e))
            }
        }
    }

    fun updateProfile(userId: String, profileData: Map<String, Any>) {
        if (userId.isEmpty()) {
            _updateResult.postValue(Result.failure(Exception("User ID kosong")))
            return
        }

        // Validate profile data
        val validationResult = validateProfileData(profileData)
        if (!validationResult.first) {
            _updateResult.postValue(Result.failure(Exception(validationResult.second)))
            return
        }

        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val result = userRepository.updateUserProfile(userId, profileData)
                result.onSuccess {
                    _updateResult.postValue(Result.success("✅ Profil berhasil diperbarui"))
                    // Reload profile data untuk refresh UI
                    loadUserProfile(userId)
                }.onFailure { error ->
                    _updateResult.postValue(Result.failure(error))
                }
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            } finally {
                _isLoading.postValue(false)
            }
        }
    }

    private fun validateProfileData(profileData: Map<String, Any>): Pair<Boolean, String> {
        val fullName = profileData["fullName"]?.toString()?.trim() ?: ""
        val phone = profileData["phone"]?.toString()?.trim() ?: ""
        val dateOfBirth = profileData["dateOfBirth"]?.toString()?.trim() ?: ""

        return when {
            fullName.isEmpty() -> Pair(false, "Nama lengkap wajib diisi")
            fullName.length < 3 -> Pair(false, "Nama lengkap minimal 3 karakter")
            phone.isEmpty() -> Pair(false, "No. telepon wajib diisi")
            phone.length < 10 -> Pair(false, "No. telepon minimal 10 digit")
            !isValidPhoneNumber(phone) -> Pair(false, "Format no. telepon tidak valid")
            dateOfBirth.isNotEmpty() && !isValidDateFormat(dateOfBirth) ->
                Pair(false, "Format tanggal lahir tidak valid (DD/MM/YYYY)")
            else -> Pair(true, "Valid")
        }
    }

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Accept formats: 08xxxxxxxxx or +62xxxxxxxxx
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
        return when {
            cleanPhone.startsWith("08") && cleanPhone.length >= 11 -> true
            cleanPhone.startsWith("+628") && cleanPhone.length >= 13 -> true
            cleanPhone.startsWith("628") && cleanPhone.length >= 12 -> true
            else -> false
        }
    }

    private fun isValidDateFormat(date: String): Boolean {
        return try {
            val regex = Regex("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$")
            if (!regex.matches(date)) return false

            // Additional validation: check if date is valid
            val parts = date.split("/")
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()

            // Basic date validation
            when {
                year < 1900 || year > 2024 -> false
                month < 1 || month > 12 -> false
                day < 1 || day > 31 -> false
                month == 2 && day > 29 -> false // February
                (month == 4 || month == 6 || month == 9 || month == 11) && day > 30 -> false
                else -> true
            }
        } catch (e: Exception) {
            false
        }
    }

    // Helper method to check if user profile is complete
    fun isProfileComplete(user: User?): Boolean {
        return user?.let {
            it.fullName.isNotEmpty() &&
                    it.phone.isNotEmpty() &&
                    it.dateOfBirth.isNotEmpty() &&
                    it.gender.isNotEmpty()
        } ?: false
    }

    // Helper method to get profile completion percentage
    fun getProfileCompletionPercentage(user: User?): Int {
        if (user == null) return 0

        val fields = listOf(
            user.fullName,
            user.phone,
            user.dateOfBirth,
            user.gender,
            user.address,
            user.emergencyContact,
            user.emergencyPhone,
            user.bloodType
        )

        val filledFields = fields.count { it.isNotEmpty() }
        return (filledFields * 100) / fields.size
    }
}