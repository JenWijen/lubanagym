package com.duta.lubanagym.utils

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.data.repository.MemberRepository
import com.duta.lubanagym.data.repository.StaffRepository
// HAPUS import TrainerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ProfileSyncHelper(private val firebaseService: FirebaseService) {

    private val memberRepository = MemberRepository(firebaseService)
    private val staffRepository = StaffRepository(firebaseService)
    // HAPUS trainerRepository

    fun syncUserProfileUpdate(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (user.role) {
                    Constants.ROLE_MEMBER -> syncMemberProfile(user)
                    Constants.ROLE_STAFF -> syncStaffProfile(user)
                    // HAPUS Constants.ROLE_TRAINER -> syncTrainerProfile(user)
                    // Constants.ROLE_ADMIN, Constants.ROLE_GUEST -> No sync needed
                }
            } catch (e: Exception) {
                println("Profile sync error: ${e.message}")
            }
        }
    }

    private suspend fun syncMemberProfile(user: User) {
        memberRepository.getMemberByUserId(user.id).onSuccess { member ->
            member?.let {
                val updates = mapOf(
                    "name" to getDisplayName(user),
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
                    "name" to getDisplayName(user),
                    "phone" to user.phone,
                    "profileImageUrl" to user.profileImageUrl,
                    "updatedAt" to System.currentTimeMillis()
                )
                staffRepository.updateStaff(it.id, updates)
            }
        }
    }

    // HAPUS syncTrainerProfile method

    // Enhanced sync with full profile data
    fun syncFullUserProfile(user: User) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (user.role) {
                    Constants.ROLE_MEMBER -> syncFullMemberProfile(user)
                    Constants.ROLE_STAFF -> syncFullStaffProfile(user)
                    // HAPUS Constants.ROLE_TRAINER -> syncFullTrainerProfile(user)
                }
            } catch (e: Exception) {
                println("Full profile sync error: ${e.message}")
            }
        }
    }

    private suspend fun syncFullMemberProfile(user: User) {
        memberRepository.getMemberByUserId(user.id).onSuccess { member ->
            member?.let {
                val updates = mutableMapOf<String, Any>(
                    "name" to getDisplayName(user),
                    "phone" to user.phone,
                    "profileImageUrl" to user.profileImageUrl
                )

                // Add extended profile data as metadata
                if (user.address.isNotEmpty()) {
                    updates["address"] = user.address
                }
                if (user.emergencyContact.isNotEmpty()) {
                    updates["emergencyContact"] = user.emergencyContact
                }
                if (user.emergencyPhone.isNotEmpty()) {
                    updates["emergencyPhone"] = user.emergencyPhone
                }
                if (user.bloodType.isNotEmpty()) {
                    updates["bloodType"] = user.bloodType
                }
                if (user.allergies.isNotEmpty()) {
                    updates["allergies"] = user.allergies
                }
                if (user.dateOfBirth.isNotEmpty()) {
                    updates["dateOfBirth"] = user.dateOfBirth
                }
                if (user.gender.isNotEmpty()) {
                    updates["gender"] = user.gender
                }

                memberRepository.updateMember(it.id, updates)
            }
        }
    }

    private suspend fun syncFullStaffProfile(user: User) {
        staffRepository.getStaffByUserId(user.id).onSuccess { staff ->
            staff?.let {
                val updates = mutableMapOf<String, Any>(
                    "name" to getDisplayName(user),
                    "phone" to user.phone,
                    "profileImageUrl" to user.profileImageUrl,
                    "updatedAt" to System.currentTimeMillis()
                )

                // Add extended profile data
                if (user.address.isNotEmpty()) {
                    updates["address"] = user.address
                }
                if (user.emergencyContact.isNotEmpty()) {
                    updates["emergencyContact"] = user.emergencyContact
                }
                if (user.emergencyPhone.isNotEmpty()) {
                    updates["emergencyPhone"] = user.emergencyPhone
                }
                if (user.dateOfBirth.isNotEmpty()) {
                    updates["dateOfBirth"] = user.dateOfBirth
                }
                if (user.gender.isNotEmpty()) {
                    updates["gender"] = user.gender
                }

                staffRepository.updateStaff(it.id, updates)
            }
        }
    }

    // HAPUS syncFullTrainerProfile method

    private fun getDisplayName(user: User): String {
        return if (user.fullName.isNotEmpty()) user.fullName else user.username
    }

    // Method to sync when user profile is updated
    suspend fun onUserProfileUpdated(userId: String): Result<Unit> {
        return try {
            // Get latest user data
            val userDoc = firebaseService.getDocument(Constants.USERS_COLLECTION, userId)
            if (userDoc.exists()) {
                val user = User(
                    id = userDoc.id,
                    email = userDoc.getString("email") ?: "",
                    username = userDoc.getString("username") ?: "",
                    role = userDoc.getString("role") ?: Constants.ROLE_MEMBER,
                    createdAt = userDoc.getLong("createdAt") ?: 0L,
                    fullName = userDoc.getString("fullName") ?: "",
                    phone = userDoc.getString("phone") ?: "",
                    dateOfBirth = userDoc.getString("dateOfBirth") ?: "",
                    gender = userDoc.getString("gender") ?: "",
                    address = userDoc.getString("address") ?: "",
                    profileImageUrl = userDoc.getString("profileImageUrl") ?: "",
                    emergencyContact = userDoc.getString("emergencyContact") ?: "",
                    emergencyPhone = userDoc.getString("emergencyPhone") ?: "",
                    bloodType = userDoc.getString("bloodType") ?: "",
                    allergies = userDoc.getString("allergies") ?: "",
                    isProfileComplete = userDoc.getBoolean("isProfileComplete") ?: false,
                    updatedAt = userDoc.getLong("updatedAt") ?: 0L
                )

                syncFullUserProfile(user)
                Result.success(Unit)
            } else {
                Result.failure(Exception("User not found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}