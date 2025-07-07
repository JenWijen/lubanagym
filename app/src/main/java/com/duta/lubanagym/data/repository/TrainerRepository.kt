package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Trainer
import com.duta.lubanagym.utils.Constants
import kotlinx.coroutines.tasks.await

class TrainerRepository(private val firebaseService: FirebaseService) {

    suspend fun createTrainer(trainer: Trainer): Result<String> {
        return try {
            val trainerMap = mapOf(
                "userId" to trainer.userId, // Tambah userId
                "name" to trainer.name,
                "specialization" to trainer.specialization,
                "experience" to trainer.experience,
                "phone" to trainer.phone,
                "bio" to trainer.bio,
                "profileImageUrl" to trainer.profileImageUrl,
                "isActive" to trainer.isActive,
                "createdAt" to trainer.createdAt
            )

            val docRef = firebaseService.addDocument(Constants.TRAINERS_COLLECTION, trainerMap)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTrainers(): Result<List<Trainer>> {
        return try {
            val snapshot = firebaseService.getCollection(Constants.TRAINERS_COLLECTION)
            val trainers = snapshot.documents.mapNotNull { doc ->
                Trainer(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "", // Tambah userId
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    experience = doc.getString("experience") ?: "",
                    phone = doc.getString("phone") ?: "",
                    bio = doc.getString("bio") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.success(trainers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW METHOD - Get trainer by userId
    suspend fun getTrainerByUserId(userId: String): Result<Trainer?> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(Constants.TRAINERS_COLLECTION, "userId", userId)
            if (snapshot.documents.isEmpty()) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val trainer = Trainer(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    experience = doc.getString("experience") ?: "",
                    phone = doc.getString("phone") ?: "",
                    bio = doc.getString("bio") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
                Result.success(trainer)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTrainer(trainerId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firebaseService.updateDocument(Constants.TRAINERS_COLLECTION, trainerId, updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTrainer(trainerId: String): Result<Unit> {
        return try {
            firebaseService.deleteDocument(Constants.TRAINERS_COLLECTION, trainerId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW METHOD - Delete trainer by userId (untuk cleanup role change)
    suspend fun deleteTrainerByUserId(userId: String): Result<Unit> {
        return try {
            val trainerResult = getTrainerByUserId(userId)
            trainerResult.onSuccess { trainer ->
                if (trainer != null) {
                    return deleteTrainer(trainer.id)
                } else {
                    return Result.success(Unit) // No trainer to delete
                }
            }.onFailure { error ->
                return Result.failure(error)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTrainerById(trainerId: String): Result<Trainer?> {
        return try {
            val doc = firebaseService.getDocument(Constants.TRAINERS_COLLECTION, trainerId)
            if (doc.exists()) {
                val trainer = Trainer(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    experience = doc.getString("experience") ?: "",
                    phone = doc.getString("phone") ?: "",
                    bio = doc.getString("bio") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
                Result.success(trainer)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}