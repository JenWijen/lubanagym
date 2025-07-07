package com.duta.lubanagym.utils

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.repository.MemberRepository
import com.duta.lubanagym.data.repository.StaffRepository
import com.duta.lubanagym.data.repository.TrainerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileSyncHelper(private val firebaseService: FirebaseService) {

    private val memberRepository = MemberRepository(firebaseService)
    private val staffRepository = StaffRepository(firebaseService)
    private val trainerRepository = TrainerRepository(firebaseService)

    fun syncUserProfileUpdate(user: User) {
        // Use coroutine scope for async operations
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (user.role) {
                    Constants.ROLE_MEMBER -> syncMemberProfile(user)
                    Constants.ROLE_STAFF -> syncStaffProfile(user)
                    Constants.ROLE_TRAINER -> syncTrainerProfile(user)
                    // Admin tidak perlu sync collection terpisah
                }
            } catch (e: Exception) {
                // Log error but don't fail the main update
                println("Profile sync error: ${e.message}")
            }
        }
    }

    private suspend fun syncMemberProfile(user: User) {
        memberRepository.getMemberByUserId(user.id).onSuccess { member ->
            member?.let {
                val updates = mapOf(
                    "name" to (user.fullName.ifEmpty { user.username }),
                    "phone" to user.phone,
                    "profileImageUrl" to user.profileImageUrl
                )
                memberRepository.updateMember(it.id, updates)
            }
        }
    }

    private suspend fun syncStaffProfile(user: User) {
        staffRepository.getStaffByUserId(user.id).onSuccess { staff ->
            staff?.let {
                val updates = mapOf(
                    "name" to (user.fullName.ifEmpty { user.username }),
                    "phone" to user.phone,
                    "profileImageUrl" to user.profileImageUrl,
                    "updatedAt" to System.currentTimeMillis()
                )
                staffRepository.updateStaff(it.id, updates)
            }
        }
    }

    private suspend fun syncTrainerProfile(user: User) {
        trainerRepository.getTrainerByUserId(user.id).onSuccess { trainer ->
            trainer?.let {
                val updates = mapOf(
                    "name" to (user.fullName.ifEmpty { user.username }),
                    "phone" to user.phone,
                    "profileImageUrl" to user.profileImageUrl
                )
                trainerRepository.updateTrainer(it.id, updates)
            }
        }
    }
}