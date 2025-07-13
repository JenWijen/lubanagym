package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.MemberRegistration
import com.duta.lubanagym.data.repository.MemberRegistrationRepository
import com.duta.lubanagym.data.repository.MemberRepository
import com.duta.lubanagym.data.repository.UserRepository
import kotlinx.coroutines.launch

class QRScannerViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val registrationRepository = MemberRegistrationRepository(firebaseService)
    private val memberRepository = MemberRepository(firebaseService)
    private val userRepository = UserRepository(firebaseService)

    private val _scanResult = MutableLiveData<Result<MemberRegistration>>()
    val scanResult: LiveData<Result<MemberRegistration>> = _scanResult

    private val _activationResult = MutableLiveData<Result<String>>()
    val activationResult: LiveData<Result<String>> = _activationResult

    fun processQRCode(qrCode: String, adminId: String) {
        viewModelScope.launch {
            try {
                // Validate QR code format
                if (!qrCode.startsWith("LUBANA_REG_")) {
                    _scanResult.postValue(Result.failure(Exception("QR Code tidak valid. Pastikan ini adalah QR Code pendaftaran member Lubana Gym.")))
                    return@launch
                }

                // Get registration by QR code
                val result = registrationRepository.getRegistrationByQrCode(qrCode)
                result.onSuccess { registration ->
                    if (registration == null) {
                        _scanResult.postValue(Result.failure(Exception("QR Code tidak ditemukan dalam sistem.")))
                        return@onSuccess
                    }

                    // Check if already activated
                    if (registration.status == "activated") {
                        _scanResult.postValue(Result.failure(Exception("Member sudah diaktivasi sebelumnya pada ${java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale("id", "ID")).format(java.util.Date(registration.activationDate))}")))
                        return@onSuccess
                    }

                    // Check if expired
                    val currentTime = System.currentTimeMillis()
                    if (currentTime > registration.expiryDate) {
                        _scanResult.postValue(Result.failure(Exception("QR Code sudah expired. Member harus mendaftar ulang.")))
                        return@onSuccess
                    }

                    // Valid registration
                    _scanResult.postValue(Result.success(registration))
                }.onFailure { error ->
                    _scanResult.postValue(Result.failure(error))
                }

            } catch (e: Exception) {
                _scanResult.postValue(Result.failure(e))
            }
        }
    }

    fun activateMember(registrationId: String, activatedBy: String) {
        viewModelScope.launch {
            try {
                val result = registrationRepository.activateRegistration(
                    registrationId,
                    activatedBy,
                    memberRepository,
                    userRepository
                )

                result.onSuccess { message ->
                    _activationResult.postValue(Result.success("🎉 $message\n\nMember sekarang dapat menggunakan semua fasilitas gym dengan akun yang sama."))
                }.onFailure { error ->
                    _activationResult.postValue(Result.failure(error))
                }

            } catch (e: Exception) {
                _activationResult.postValue(Result.failure(e))
            }
        }
    }
}