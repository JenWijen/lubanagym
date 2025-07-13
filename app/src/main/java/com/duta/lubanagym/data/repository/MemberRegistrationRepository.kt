// Updated MemberRegistrationRepository.kt - Fixed profile sync after QR scan
package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.MemberRegistration
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.utils.Constants
import java.util.*

class MemberRegistrationRepository(private val firebaseService: FirebaseService) {

    companion object {
        const val MEMBER_REGISTRATIONS_COLLECTION = "member_registrations"
    }

    suspend fun createRegistration(registration: MemberRegistration): Result<String> {
        return try {
            val registrationMap = mapOf(
                "userId" to registration.userId,
                "userName" to registration.userName,
                "userEmail" to registration.userEmail,
                "userPhone" to registration.userPhone,
                "membershipType" to registration.membershipType,
                "duration" to registration.duration,
                "price" to registration.price,
                "registrationDate" to registration.registrationDate,
                "expiryDate" to registration.expiryDate,
                "activationDate" to registration.activationDate,
                "status" to registration.status,
                "qrCode" to registration.qrCode,
                "isActive" to registration.isActive,
                "activatedBy" to registration.activatedBy,

                // Extended user data
                "userFullName" to registration.userFullName,
                "userAddress" to registration.userAddress,
                "userDateOfBirth" to registration.userDateOfBirth,
                "userGender" to registration.userGender,
                "userEmergencyContact" to registration.userEmergencyContact,
                "userEmergencyPhone" to registration.userEmergencyPhone,
                "userBloodType" to registration.userBloodType,
                "userAllergies" to registration.userAllergies
            )

            val docRef = firebaseService.addDocument(MEMBER_REGISTRATIONS_COLLECTION, registrationMap)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRegistrationByUserId(userId: String): Result<MemberRegistration?> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(
                MEMBER_REGISTRATIONS_COLLECTION,
                "userId",
                userId
            )

            if (snapshot.documents.isEmpty()) {
                Result.success(null)
            } else {
                // Get the most recent registration
                val doc = snapshot.documents.maxByOrNull {
                    it.getLong("registrationDate") ?: 0L
                }!!

                val registration = MemberRegistration(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    userPhone = doc.getString("userPhone") ?: "",
                    membershipType = doc.getString("membershipType") ?: "",
                    duration = doc.getLong("duration")?.toInt() ?: 0,
                    price = doc.getLong("price")?.toInt() ?: 0,
                    registrationDate = doc.getLong("registrationDate") ?: 0L,
                    expiryDate = doc.getLong("expiryDate") ?: 0L,
                    activationDate = doc.getLong("activationDate") ?: 0L,
                    status = doc.getString("status") ?: "pending",
                    qrCode = doc.getString("qrCode") ?: "",
                    isActive = doc.getBoolean("isActive") ?: false,
                    activatedBy = doc.getString("activatedBy") ?: "",

                    // Extended user data
                    userFullName = doc.getString("userFullName") ?: "",
                    userAddress = doc.getString("userAddress") ?: "",
                    userDateOfBirth = doc.getString("userDateOfBirth") ?: "",
                    userGender = doc.getString("userGender") ?: "",
                    userEmergencyContact = doc.getString("userEmergencyContact") ?: "",
                    userEmergencyPhone = doc.getString("userEmergencyPhone") ?: "",
                    userBloodType = doc.getString("userBloodType") ?: "",
                    userAllergies = doc.getString("userAllergies") ?: ""
                )

                Result.success(registration)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // FIXED: Enhanced activation with proper profile sync
    suspend fun activateRegistration(
        registrationId: String,
        activatedBy: String,
        memberRepository: MemberRepository,
        userRepository: UserRepository
    ): Result<String> {
        return try {
            // Get registration data
            val doc = firebaseService.getDocument(MEMBER_REGISTRATIONS_COLLECTION, registrationId)
            if (!doc.exists()) {
                return Result.failure(Exception("Registration tidak ditemukan"))
            }

            val registration = MemberRegistration(
                id = doc.id,
                userId = doc.getString("userId") ?: "",
                userName = doc.getString("userName") ?: "",
                userEmail = doc.getString("userEmail") ?: "",
                userPhone = doc.getString("userPhone") ?: "",
                membershipType = doc.getString("membershipType") ?: "",
                duration = doc.getLong("duration")?.toInt() ?: 0,
                price = doc.getLong("price")?.toInt() ?: 0,
                registrationDate = doc.getLong("registrationDate") ?: 0L,
                expiryDate = doc.getLong("expiryDate") ?: 0L,
                status = doc.getString("status") ?: "pending",
                qrCode = doc.getString("qrCode") ?: "",
                userFullName = doc.getString("userFullName") ?: "",
                userAddress = doc.getString("userAddress") ?: "",
                userDateOfBirth = doc.getString("userDateOfBirth") ?: "",
                userGender = doc.getString("userGender") ?: "",
                userEmergencyContact = doc.getString("userEmergencyContact") ?: "",
                userEmergencyPhone = doc.getString("userEmergencyPhone") ?: "",
                userBloodType = doc.getString("userBloodType") ?: "",
                userAllergies = doc.getString("userAllergies") ?: ""
            )

            // Check if still valid (within 5 days)
            val currentTime = System.currentTimeMillis()
            if (currentTime > registration.expiryDate) {
                return Result.failure(Exception("QR Code sudah expired"))
            }

            // FIXED: Get latest user profile data before creating member
            val userResult = userRepository.getUserById(registration.userId)
            if (userResult.isFailure) {
                return Result.failure(Exception("User tidak ditemukan"))
            }

            val currentUser = userResult.getOrThrow()

            // FIXED: Update user role to member first
            val userUpdateResult = userRepository.updateUserRole(registration.userId, Constants.ROLE_MEMBER)
            if (userUpdateResult.isFailure) {
                return Result.failure(Exception("Gagal update role user"))
            }

            // Calculate membership end date
            val membershipEndDate = Calendar.getInstance().apply {
                timeInMillis = currentTime
                add(Calendar.MONTH, registration.duration)
            }.timeInMillis

            // FIXED: Create member profile with latest user data including Google profile image
            val member = Member(
                userId = registration.userId,
                name = if (currentUser.fullName.isNotEmpty()) currentUser.fullName else registration.userName,
                phone = if (currentUser.phone.isNotEmpty()) currentUser.phone else registration.userPhone,
                membershipType = registration.membershipType,
                joinDate = currentTime,
                expiryDate = membershipEndDate,
                isActive = true,
                profileImageUrl = currentUser.profileImageUrl, // FIXED: Use current user's profile image
                qrCode = "LUBANA_MEMBER_${registration.userId.take(8).uppercase()}",

                // FIXED: Use latest user profile data
                address = if (currentUser.address.isNotEmpty()) currentUser.address else registration.userAddress,
                emergencyContact = if (currentUser.emergencyContact.isNotEmpty()) currentUser.emergencyContact else registration.userEmergencyContact,
                emergencyPhone = if (currentUser.emergencyPhone.isNotEmpty()) currentUser.emergencyPhone else registration.userEmergencyPhone,
                bloodType = if (currentUser.bloodType.isNotEmpty()) currentUser.bloodType else registration.userBloodType,
                allergies = if (currentUser.allergies.isNotEmpty()) currentUser.allergies else registration.userAllergies,
                dateOfBirth = if (currentUser.dateOfBirth.isNotEmpty()) currentUser.dateOfBirth else registration.userDateOfBirth,
                gender = if (currentUser.gender.isNotEmpty()) currentUser.gender else registration.userGender
            )

            val memberResult = memberRepository.createMember(member)
            if (memberResult.isFailure) {
                return Result.failure(Exception("Gagal membuat profil member"))
            }

            // Update registration status
            val updates = mapOf(
                "status" to "activated",
                "isActive" to true,
                "activationDate" to currentTime,
                "activatedBy" to activatedBy
            )

            firebaseService.updateDocument(MEMBER_REGISTRATIONS_COLLECTION, registrationId, updates)

            Result.success("Member berhasil diaktivasi dengan data profil terbaru")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPendingRegistrations(): Result<List<MemberRegistration>> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(
                MEMBER_REGISTRATIONS_COLLECTION,
                "status",
                "pending"
            )

            val registrations = snapshot.documents.mapNotNull { doc ->
                MemberRegistration(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    userPhone = doc.getString("userPhone") ?: "",
                    membershipType = doc.getString("membershipType") ?: "",
                    duration = doc.getLong("duration")?.toInt() ?: 0,
                    price = doc.getLong("price")?.toInt() ?: 0,
                    registrationDate = doc.getLong("registrationDate") ?: 0L,
                    expiryDate = doc.getLong("expiryDate") ?: 0L,
                    activationDate = doc.getLong("activationDate") ?: 0L,
                    status = doc.getString("status") ?: "pending",
                    qrCode = doc.getString("qrCode") ?: "",
                    isActive = doc.getBoolean("isActive") ?: false,
                    activatedBy = doc.getString("activatedBy") ?: "",
                    userFullName = doc.getString("userFullName") ?: "",
                )
            }

            Result.success(registrations)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getRegistrationByQrCode(qrCode: String): Result<MemberRegistration?> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(
                MEMBER_REGISTRATIONS_COLLECTION,
                "qrCode",
                qrCode
            )

            if (snapshot.documents.isEmpty()) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val registration = MemberRegistration(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    userName = doc.getString("userName") ?: "",
                    userEmail = doc.getString("userEmail") ?: "",
                    userPhone = doc.getString("userPhone") ?: "",
                    membershipType = doc.getString("membershipType") ?: "",
                    duration = doc.getLong("duration")?.toInt() ?: 0,
                    price = doc.getLong("price")?.toInt() ?: 0,
                    registrationDate = doc.getLong("registrationDate") ?: 0L,
                    expiryDate = doc.getLong("expiryDate") ?: 0L,
                    activationDate = doc.getLong("activationDate") ?: 0L,
                    status = doc.getString("status") ?: "pending",
                    qrCode = doc.getString("qrCode") ?: "",
                    isActive = doc.getBoolean("isActive") ?: false,
                    activatedBy = doc.getString("activatedBy") ?: "",
                    userFullName = doc.getString("userFullName") ?: ""
                )

                Result.success(registration)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}