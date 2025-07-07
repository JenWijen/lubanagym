package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.utils.Constants

class EquipmentRepository(private val firebaseService: FirebaseService) {

    suspend fun createEquipment(equipment: Equipment): Result<String> {
        return try {
            val equipmentMap = mapOf(
                "id" to equipment.id,
                "name" to equipment.name,
                "description" to equipment.description,
                "category" to equipment.category,
                "imageUrl" to equipment.imageUrl,
                "instructions" to equipment.instructions,
                "isAvailable" to equipment.isAvailable,
                "createdAt" to equipment.createdAt
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
                    id = doc.getString("id") ?: "",
                    name = doc.getString("name") ?: "",
                    description = doc.getString("description") ?: "",
                    category = doc.getString("category") ?: "",
                    imageUrl = doc.getString("imageUrl") ?: "",
                    instructions = doc.getString("instructions") ?: "",
                    isAvailable = doc.getBoolean("isAvailable") ?: true,
                    createdAt = doc.getLong("createdAt") ?: 0L
                )
            }
            Result.success(equipment)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateEquipment(equipmentId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firebaseService.updateDocument(Constants.EQUIPMENT_COLLECTION, equipmentId, updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteEquipment(equipmentId: String): Result<Unit> {
        return try {
            firebaseService.deleteDocument(Constants.EQUIPMENT_COLLECTION, equipmentId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}