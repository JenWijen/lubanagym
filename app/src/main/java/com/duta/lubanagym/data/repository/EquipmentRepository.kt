package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.utils.Constants

class EquipmentRepository(private val firebaseService: FirebaseService) {

    suspend fun createEquipment(equipment: Equipment): Result<String> {
        return try {
            val equipmentMap = mapOf(
                "name" to equipment.name,
                "description" to equipment.description,
                "category" to equipment.category,
                "imageUrl" to equipment.imageUrl,
                "instructions" to equipment.instructions,
                "isAvailable" to equipment.isAvailable,
                "createdAt" to equipment.createdAt,
                "updatedAt" to equipment.updatedAt
            )

            val docRef = firebaseService.addDocument(Constants.EQUIPMENT_COLLECTION, equipmentMap)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllEquipment(): Result<List<Equipment>> {
        return try {
            val snapshot = firebaseService.getCollection(Constants.EQUIPMENT_COLLECTION)
            val equipment = snapshot.documents.mapNotNull { doc ->
                Equipment(
                    id = doc.id, // FIXED: Use document ID from Firestore
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    instructions = doc.getString("instructions") ?: "",
                    isAvailable = doc.getBoolean("isAvailable") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
            }
            Result.success(equipment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEquipment(equipmentId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            // FIXED: Add validation for equipmentId
            if (equipmentId.isEmpty()) {
                return Result.failure(Exception("Equipment ID tidak boleh kosong"))
            }

            // FIXED: Add updatedAt timestamp
            val updatedData = updates.toMutableMap()
            updatedData["updatedAt"] = System.currentTimeMillis()

            firebaseService.updateDocument(Constants.EQUIPMENT_COLLECTION, equipmentId, updatedData)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal update equipment: ${e.message}"))
        }
    }

    suspend fun deleteEquipment(equipmentId: String): Result<Unit> {
        return try {
            // FIXED: Add validation for equipmentId
            if (equipmentId.isEmpty()) {
                return Result.failure(Exception("Equipment ID tidak boleh kosong"))
            }

            firebaseService.deleteDocument(Constants.EQUIPMENT_COLLECTION, equipmentId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Gagal hapus equipment: ${e.message}"))
        }
    }

    // FIXED: Add method to get equipment by ID
    suspend fun getEquipmentById(equipmentId: String): Result<Equipment?> {
        return try {
            if (equipmentId.isEmpty()) {
                return Result.failure(Exception("Equipment ID tidak boleh kosong"))
            }

            val doc = firebaseService.getDocument(Constants.EQUIPMENT_COLLECTION, equipmentId)
            if (doc.exists()) {
                val equipment = Equipment(
                    id = doc.id,
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    instructions = doc.getString("instructions") ?: "",
                    isAvailable = doc.getBoolean("isAvailable") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    updatedAt = doc.getLong("updatedAt") ?: System.currentTimeMillis()
                )
                Result.success(equipment)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mengambil data equipment: ${e.message}"))
        }
    }
}