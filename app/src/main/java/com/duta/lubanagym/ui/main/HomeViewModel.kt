package com.duta.lubanagym.ui.main

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.repository.MemberRepository
import com.duta.lubanagym.data.repository.TrainerRepository
import com.duta.lubanagym.data.repository.EquipmentRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.async

// Data class untuk gym statistics
data class GymStats(
    val activeMembers: Int = 0,
    val activeTrainers: Int = 0,
    val availableEquipment: Int = 0,
    val totalUsers: Int = 0
)

class HomeViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val memberRepository = MemberRepository(firebaseService)
    private val trainerRepository = TrainerRepository(firebaseService)
    private val equipmentRepository = EquipmentRepository(firebaseService)

    private val _gymStats = MutableLiveData<Result<GymStats>>()
    val gymStats: LiveData<Result<GymStats>> = _gymStats

    fun loadGymStats() {
        viewModelScope.launch {
            try {
                // Load semua data secara parallel
                val membersDeferred = async { memberRepository.getAllMembers() }
                val trainersDeferred = async { trainerRepository.getAllTrainers() }
                val equipmentDeferred = async { equipmentRepository.getAllEquipment() }

                // Ambil hasil
                val membersResult = membersDeferred.await()
                val trainersResult = trainersDeferred.await()
                val equipmentResult = equipmentDeferred.await()

                // Hitung stats
                val activeMembers = membersResult.getOrNull()?.count { it.isActive } ?: 0
                val activeTrainers = trainersResult.getOrNull()?.count { it.isActive } ?: 0
                val availableEquipment = equipmentResult.getOrNull()?.count { it.isAvailable } ?: 0

                val stats = GymStats(
                    activeMembers = activeMembers,
                    activeTrainers = activeTrainers,
                    availableEquipment = availableEquipment
                )

                _gymStats.postValue(Result.success(stats))

            } catch (e: Exception) {
                _gymStats.postValue(Result.failure(e))
            }
        }
    }
}