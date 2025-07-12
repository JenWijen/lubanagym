package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.utils.Constants
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential

class AuthRepository(private val firebaseService: FirebaseService) {

    // UPDATED: Register tanpa token
    suspend fun register(email: String, password: String, username: String): Result<String> {
        return try {
            // Register user langsung tanpa verifikasi token
            val authResult = firebaseService.signUp(email, password)
            val userId = authResult.user?.uid ?: throw Exception("Gagal mendapatkan user ID")

            // Create user document dengan role guest sebagai default
            val user = User(
                id = userId,
                email = email,
                username = username,
                role = Constants.ROLE_GUEST, // Default sebagai guest
                createdAt = System.currentTimeMillis(),
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
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Google Sign-In
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            val credential: AuthCredential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = firebaseService.signInWithGoogle(credential)
            val userId = authResult.user?.uid ?: throw Exception("Gagal mendapatkan user ID")
            val userEmail = authResult.user?.email ?: throw Exception("Gagal mendapatkan email")
            val userName = authResult.user?.displayName ?: userEmail.substringBefore("@")
            val profileImageUrl = authResult.user?.photoUrl?.toString() ?: ""

            // Check if user already exists
            val existingUserDoc = firebaseService.getDocument(Constants.USERS_COLLECTION, userId)

            val user = if (existingUserDoc.exists()) {
                // User sudah ada, return existing user
                User(
                    id = existingUserDoc.id,
                    email = existingUserDoc.getString("email") ?: userEmail,
                    username = existingUserDoc.getString("username") ?: userName,
                    role = existingUserDoc.getString("role") ?: Constants.ROLE_GUEST,
                    createdAt = existingUserDoc.getLong("createdAt") ?: System.currentTimeMillis(),
                    fullName = existingUserDoc.getString("fullName") ?: userName,
                    phone = existingUserDoc.getString("phone") ?: "",
                    dateOfBirth = existingUserDoc.getString("dateOfBirth") ?: "",
                    gender = existingUserDoc.getString("gender") ?: "",
                    address = existingUserDoc.getString("address") ?: "",
                    profileImageUrl = existingUserDoc.getString("profileImageUrl") ?: profileImageUrl,
                    emergencyContact = existingUserDoc.getString("emergencyContact") ?: "",
                    emergencyPhone = existingUserDoc.getString("emergencyPhone") ?: "",
                    bloodType = existingUserDoc.getString("bloodType") ?: "",
                    allergies = existingUserDoc.getString("allergies") ?: "",
                    isProfileComplete = existingUserDoc.getBoolean("isProfileComplete") ?: false,
                    updatedAt = existingUserDoc.getLong("updatedAt") ?: 0L
                )
            } else {
                // User baru, create new user document
                val newUser = User(
                    id = userId,
                    email = userEmail,
                    username = userName,
                    role = Constants.ROLE_GUEST,
                    createdAt = System.currentTimeMillis(),
                    fullName = userName,
                    profileImageUrl = profileImageUrl,
                    isProfileComplete = false
                )

                val userMap = mapOf(
                    "email" to newUser.email,
                    "username" to newUser.username,
                    "role" to newUser.role,
                    "createdAt" to newUser.createdAt,
                    "fullName" to newUser.fullName,
                    "phone" to newUser.phone,
                    "dateOfBirth" to newUser.dateOfBirth,
                    "gender" to newUser.gender,
                    "address" to newUser.address,
                    "profileImageUrl" to newUser.profileImageUrl,
                    "emergencyContact" to newUser.emergencyContact,
                    "emergencyPhone" to newUser.emergencyPhone,
                    "bloodType" to newUser.bloodType,
                    "allergies" to newUser.allergies,
                    "isProfileComplete" to newUser.isProfileComplete,
                    "updatedAt" to newUser.updatedAt
                )

                firebaseService.addDocumentWithId(Constants.USERS_COLLECTION, userId, userMap)
                newUser
            }

            Result.success(user)
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