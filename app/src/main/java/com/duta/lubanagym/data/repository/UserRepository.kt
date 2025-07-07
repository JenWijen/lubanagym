package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.utils.Constants
import kotlinx.coroutines.tasks.await

class UserRepository(private val firebaseService: FirebaseService) {

    suspend fun getAllUsers(): Result<List<User>> {
        return try {
            val snapshot = firebaseService.getCollection(Constants.USERS_COLLECTION)
            val users = snapshot.documents.mapNotNull { doc ->
                User(
                    id = doc.id,
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username") ?: "",
                    role = doc.getString("role") ?: Constants.ROLE_MEMBER,
                    createdAt = doc.getLong("createdAt") ?: 0L,

                    // Extended fields
                    fullName = doc.getString("fullName") ?: "",
                    phone = doc.getString("phone") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: "",
                    address = doc.getString("address") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    bloodType = doc.getString("bloodType") ?: "",
                    allergies = doc.getString("allergies") ?: "",
                    isProfileComplete = doc.getBoolean("isProfileComplete") ?: false,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
            }
            Result.success(users)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserRole(userId: String, newRole: String): Result<Unit> {
        return try {
            val updates = mapOf(
                "role" to newRole,
                "updatedAt" to System.currentTimeMillis()
            )
            firebaseService.updateDocument(Constants.USERS_COLLECTION, userId, updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(userId: String): Result<User> {
        return try {
            val doc = firebaseService.getDocument(Constants.USERS_COLLECTION, userId)
            if (!doc.exists()) {
                throw Exception("User tidak ditemukan")
            }

            val user = User(
                id = doc.id,
                email = doc.getString("email") ?: "",
                username = doc.getString("username") ?: "",
                role = doc.getString("role") ?: Constants.ROLE_MEMBER,
                createdAt = doc.getLong("createdAt") ?: 0L,

                // Extended fields
                fullName = doc.getString("fullName") ?: "",
                phone = doc.getString("phone") ?: "",
                dateOfBirth = doc.getString("dateOfBirth") ?: "",
                gender = doc.getString("gender") ?: "",
                address = doc.getString("address") ?: "",
                profileImageUrl = doc.getString("profileImageUrl") ?: "",
                emergencyContact = doc.getString("emergencyContact") ?: "",
                emergencyPhone = doc.getString("emergencyPhone") ?: "",
                bloodType = doc.getString("bloodType") ?: "",
                allergies = doc.getString("allergies") ?: "",
                isProfileComplete = doc.getBoolean("isProfileComplete") ?: false,
                updatedAt = doc.getLong("updatedAt") ?: 0L
            )

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(userId: String, profileUpdates: Map<String, Any>): Result<Unit> {
        return try {
            val updates = profileUpdates.toMutableMap()
            updates["updatedAt"] = System.currentTimeMillis()

            // Check if profile is complete - FIXED: Use proper Constants reference
            val requiredFields = Constants.getRequiredFields()
            val isComplete = requiredFields.all { field ->
                updates[field]?.toString()?.isNotEmpty() == true ||
                        // Check existing data if field not in updates
                        (profileUpdates[field] == null && getUserFieldValue(userId, field).isNotEmpty())
            }

            if (isComplete) {
                updates["isProfileComplete"] = true
            }

            firebaseService.updateDocument(Constants.USERS_COLLECTION, userId, updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getUserFieldValue(userId: String, field: String): String {
        return try {
            val doc = firebaseService.getDocument(Constants.USERS_COLLECTION, userId)
            doc.getString(field) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    suspend fun createUser(user: User): Result<String> {
        return try {
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

            val docRef = firebaseService.addDocument(Constants.USERS_COLLECTION, userMap)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserByEmail(email: String): Result<User?> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(Constants.USERS_COLLECTION, "email", email)
            if (snapshot.documents.isEmpty()) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val user = User(
                    id = doc.id,
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username") ?: "",
                    role = doc.getString("role") ?: Constants.ROLE_MEMBER,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    fullName = doc.getString("fullName") ?: "",
                    phone = doc.getString("phone") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: "",
                    address = doc.getString("address") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    bloodType = doc.getString("bloodType") ?: "",
                    allergies = doc.getString("allergies") ?: "",
                    isProfileComplete = doc.getBoolean("isProfileComplete") ?: false,
                    updatedAt = doc.getLong("updatedAt") ?: 0L
                )
                Result.success(user)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}