package com.duta.lubanagym.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.repository.UserRepository
import kotlinx.coroutines.launch

class ProfileEditViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val userRepository = UserRepository(firebaseService)

    private val _updateResult = MutableLiveData<Result<String>>()
    val updateResult: LiveData<Result<String>> = _updateResult

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    fun updateProfile(userId: String, profileData: Map<String, Any>) {
        viewModelScope.launch {
            _isLoading.postValue(true)
            try {
                val result = userRepository.updateUserProfile(userId, profileData)
                result.onSuccess {
                    _updateResult.postValue(Result.success("✅ Profil berhasil diperbarui"))
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

    fun validateProfileData(profileData: Map<String, Any>): Pair<Boolean, String> {
        val fullName = profileData["fullName"]?.toString() ?: ""
        val phone = profileData["phone"]?.toString() ?: ""
        val dateOfBirth = profileData["dateOfBirth"]?.toString() ?: ""

        return when {
            fullName.isEmpty() -> Pair(false, "Nama lengkap wajib diisi")
            fullName.length < 3 -> Pair(false, "Nama lengkap minimal 3 karakter")
            phone.isEmpty() -> Pair(false, "No. telepon wajib diisi")
            phone.length < 10 -> Pair(false, "No. telepon minimal 10 digit")
            !phone.startsWith("08") && !phone.startsWith("+62") ->
                Pair(false, "Format no. telepon tidak valid")
            dateOfBirth.isNotEmpty() && !isValidDateFormat(dateOfBirth) ->
                Pair(false, "Format tanggal lahir tidak valid (DD/MM/YYYY)")
            else -> Pair(true, "Valid")
        }
    }

    private fun isValidDateFormat(date: String): Boolean {
        return try {
            val regex = Regex("^(0[1-9]|[12][0-9]|3[01])/(0[1-9]|1[0-2])/\\d{4}$")
            regex.matches(date)
        } catch (e: Exception) {
            false
        }
    }
}