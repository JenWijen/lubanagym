package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Staff
import com.duta.lubanagym.utils.Constants
import kotlinx.coroutines.tasks.await

class StaffRepository(private val firebaseService: FirebaseService) {

    suspend fun createStaff(staff: Staff): Result<String> {
        return try {
            val staffMap = mapOf(
                "userId" to staff.userId,
                "name" to staff.name,
                "phone" to staff.phone,
                "position" to staff.position,
                "joinDate" to staff.joinDate,
                "isActive" to staff.isActive,
                "profileImageUrl" to staff.profileImageUrl,
                "createdAt" to staff.createdAt,
                "updatedAt" to staff.updatedAt,

                // Extended profile data
                "address" to staff.address,
                "emergencyContact" to staff.emergencyContact,
                "emergencyPhone" to staff.emergencyPhone,
                "dateOfBirth" to staff.dateOfBirth,
                "gender" to staff.gender
            )

            val docRef = firebaseService.addDocument(Constants.STAFF_COLLECTION, staffMap)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllStaff(): Result<List<Staff>> {
        return try {
            val snapshot = firebaseService.getCollection(Constants.STAFF_COLLECTION)
            val staff = snapshot.documents.mapNotNull { doc ->
                Staff(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    position = doc.getString("position") ?: "",
                    joinDate = doc.getLong("joinDate") ?: 0L,
                    isActive = doc.getBoolean("isActive") ?: true,
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: ""
                )
            }
            Result.success(staff)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStaffByUserId(userId: String): Result<Staff?> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(Constants.STAFF_COLLECTION, "userId", userId)
            if (snapshot.documents.isEmpty()) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val staff = Staff(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    position = doc.getString("position") ?: "",
                    joinDate = doc.getLong("joinDate") ?: 0L,
                    isActive = doc.getBoolean("isActive") ?: true,
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: ""
                )
                Result.success(staff)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateStaff(staffId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val updatedData = updates.toMutableMap()
            updatedData["updatedAt"] = System.currentTimeMillis()

            firebaseService.updateDocument(Constants.STAFF_COLLECTION, staffId, updatedData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStaff(staffId: String): Result<Unit> {
        return try {
            firebaseService.deleteDocument(Constants.STAFF_COLLECTION, staffId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteStaffByUserId(userId: String): Result<Unit> {
        return try {
            val staffResult = getStaffByUserId(userId)
            staffResult.onSuccess { staff ->
                if (staff != null) {
                    return deleteStaff(staff.id)
                } else {
                    return Result.success(Unit)
                }
            }.onFailure { error ->
                return Result.failure(error)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getStaffById(staffId: String): Result<Staff?> {
        return try {
            val doc = firebaseService.getDocument(Constants.STAFF_COLLECTION, staffId)
            if (doc.exists()) {
                val staff = Staff(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    position = doc.getString("position") ?: "",
                    joinDate = doc.getLong("joinDate") ?: 0L,
                    isActive = doc.getBoolean("isActive") ?: true,
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    createdAt = doc.getLong("createdAt") ?: System.currentTimeMillis(),
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: ""
                )
                Result.success(staff)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}