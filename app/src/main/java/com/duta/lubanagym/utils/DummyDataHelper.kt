package com.duta.lubanagym.utils

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.*
import kotlinx.coroutines.delay

class DummyDataHelper(private val firebaseService: FirebaseService) {

    suspend fun createDummyAdmin(): Result<String> {
        return try {
            // 1. Register admin account
            val adminEmail = "admin@lubanagym.com"
            val adminPassword = "admin123"

            val authResult = firebaseService.signUp(adminEmail, adminPassword)
            val adminId = authResult.user?.uid ?: throw Exception("Failed to create admin")

            // 2. Create admin user document
            val adminUser = mapOf(
                "id" to adminId,
                "email" to adminEmail,
                "username" to "Admin Lubana",
                "role" to Constants.ROLE_ADMIN,
                "createdAt" to System.currentTimeMillis()
            )

            firebaseService.addDocumentWithId(Constants.USERS_COLLECTION, adminId, adminUser)

            Result.success("Admin created: $adminEmail / $adminPassword")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    suspend fun createAllDummyData(): Result<String> {
        return try {
            val results = mutableListOf<String>()

            // Create admin
            createDummyAdmin().onSuccess { results.add(it) }
            delay(1000)

            Result.success("All dummy data created successfully!\n${results.joinToString("\n")}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}