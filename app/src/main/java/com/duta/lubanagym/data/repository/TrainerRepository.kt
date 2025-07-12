package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Trainer
import com.duta.lubanagym.utils.Constants

class TrainerRepository(private val firebaseService: FirebaseService) {

    suspend fun createTrainer(trainer: Trainer): Result<String> {
        return try {
            val trainerMap = mapOf(
                // HAPUS userId - Trainer mandiri
                "name" to trainer.name,
                "specialization" to trainer.specialization,
                "experience" to trainer.experience,
                "phone" to trainer.phone,
                "bio" to trainer.bio,
                "profileImageUrl" to trainer.profileImageUrl,
                "isActive" to trainer.isActive,
                "createdAt" to trainer.createdAt,
                "updatedAt" to trainer.updatedAt,

                // Extended profile data
                "address" to trainer.address,
                "emergencyContact" to trainer.emergencyContact,
                "emergencyPhone" to trainer.emergencyPhone,
                "dateOfBirth" to trainer.dateOfBirth,
                "gender" to trainer.gender,
                "bloodType" to trainer.bloodType,
                "allergies" to trainer.allergies,

                // Additional trainer fields
                "certification" to trainer.certification,
                "hourlyRate" to trainer.hourlyRate,
                "availability" to trainer.availability,
                "languages" to trainer.languages
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
                    // HAPUS userId mapping
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    experience = doc.getString("experience") ?: "",
                    phone = doc.getString("phone") ?: "",
                    bio = doc.getString("bio") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: "",
                    bloodType = doc.getString("bloodType") ?: "",
                    allergies = doc.getString("allergies") ?: "",

                    // Additional trainer fields
                    certification = doc.getString("certification") ?: "",
                    hourlyRate = doc.getString("hourlyRate") ?: "",
                    availability = doc.getString("availability") ?: "",
                    languages = doc.getString("languages") ?: ""
                )
            }
            Result.success(trainers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // HAPUS method getTrainerByUserId karena tidak ada userId lagi

    suspend fun updateTrainer(trainerId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val updatedData = updates.toMutableMap()
            updatedData["updatedAt"] = System.currentTimeMillis()

            firebaseService.updateDocument(Constants.TRAINERS_COLLECTION, trainerId, updatedData)
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

    // HAPUS deleteTrainerByUserId method

    suspend fun getTrainerById(trainerId: String): Result<Trainer?> {
        return try {
            val doc = firebaseService.getDocument(Constants.TRAINERS_COLLECTION, trainerId)
            if (doc.exists()) {
                val trainer = Trainer(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    experience = doc.getString("experience") ?: "",
                    phone = doc.getString("phone") ?: "",
                    bio = doc.getString("bio") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis(),

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: "",
                    bloodType = doc.getString("bloodType") ?: "",
                    allergies = doc.getString("allergies") ?: "",

                    // Additional trainer fields
                    certification = doc.getString("certification") ?: "",
                    hourlyRate = doc.getString("hourlyRate") ?: "",
                    availability = doc.getString("availability") ?: "",
                    languages = doc.getString("languages") ?: ""
                )
                Result.success(trainer)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Search trainers by specialization
    suspend fun getTrainersBySpecialization(specialization: String): Result<List<Trainer>> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(
                Constants.TRAINERS_COLLECTION,
                "specialization",
                specialization
            )

            val trainers = snapshot.documents.mapNotNull { doc ->
                // Mapping sama seperti getAllTrainers()
                Trainer(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    experience = doc.getString("experience") ?: "",
                    phone = doc.getString("phone") ?: "",
                    bio = doc.getString("bio") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
            Result.success(trainers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Get only active trainers
    suspend fun getActiveTrainers(): Result<List<Trainer>> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(
                Constants.TRAINERS_COLLECTION,
                "isActive",
                true
            )

            val trainers = snapshot.documents.mapNotNull { doc ->
                Trainer(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    specialization = doc.getString("specialization") ?: "",
                    experience = doc.getString("experience") ?: "",
                    phone = doc.getString("phone") ?: "",
                    bio = doc.getString("bio") ?: "",
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    isActive = doc.getBoolean("isActive") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
            Result.success(trainers)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}