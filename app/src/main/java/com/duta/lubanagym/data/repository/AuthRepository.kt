package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.utils.Constants

class AuthRepository(private val firebaseService: FirebaseService) {

    suspend fun register(email: String, password: String, username: String, token: String): Result<String> {
        return try {
            // Verify token first
            val tokenSnapshot = firebaseService.getCollectionWhere(Constants.TOKENS_COLLECTION, "token", token)
            if (tokenSnapshot.documents.isEmpty() || tokenSnapshot.documents[0].getBoolean("isUsed") == true) {
                return Result.failure(Exception("Token tidak valid atau sudah digunakan"))
            }

            // Register user
            val authResult = firebaseService.signUp(email, password)
            val userId = authResult.user?.uid ?: throw Exception("Gagal mendapatkan user ID")

            // Create user document with minimal data - extended profile diisi nanti
            val user = User(
                id = userId,
                email = email,
                username = username,
                role = Constants.ROLE_GUEST,
                createdAt = System.currentTimeMillis(),
                // Extended fields kosong - akan diisi di profile
                isProfileComplete = false
            )

            val userMap = mapOf(
                "email" to user.email,
                "username" to user.username,
                "role" to user.role,
                "createdAt" to user.createdAt,
                "fullName" to user.fullName,
                "phone" to user.phone,
                "dateOfBirth" to user.dateOfBirth,
                "gender" to user.gender,
                "address" to user.address,
                "profileImageUrl" to user.profileImageUrl,
                "emergencyContact" to user.emergencyContact,
                "emergencyPhone" to user.emergencyPhone,
                "bloodType" to user.bloodType,
                "allergies" to user.allergies,
                "isProfileComplete" to user.isProfileComplete,
                "updatedAt" to user.updatedAt
            )

            firebaseService.addDocumentWithId(Constants.USERS_COLLECTION, userId, userMap)

            // Mark token as used
            val tokenDoc = tokenSnapshot.documents[0]
            firebaseService.updateDocument(Constants.TOKENS_COLLECTION, tokenDoc.id,
                mapOf("isUsed" to true, "usedAt" to System.currentTimeMillis()))

            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val authResult = firebaseService.signIn(email, password)
            val userId = authResult.user?.uid ?: throw Exception("Gagal mendapatkan user ID")

            val userDoc = firebaseService.getDocument(Constants.USERS_COLLECTION, userId)
            if (!userDoc.exists()) {
                throw Exception("Data user tidak ditemukan")
            }

            val user = User(
                id = userDoc.id,
                email = userDoc.getString("email") ?: "",
                username = userDoc.getString("username") ?: "",
                role = userDoc.getString("role") ?: Constants.ROLE_GUEST,
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

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        firebaseService.signOut()
    }

    fun getCurrentUser() = firebaseService.getCurrentUser()
}