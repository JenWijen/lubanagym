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
    suspend fun createDummyTokens(): Result<String> {
        return try {
            val tokens = listOf("GYM001", "GYM002", "GYM003", "TEST123", "DEMO456")

            tokens.forEach { tokenString ->
                val tokenData = mapOf(
                    "token" to tokenString,
                    "isUsed" to false,
                    "createdAt" to System.currentTimeMillis(),
                    "usedAt" to 0L,
                    "createdBy" to "system"
                )
                firebaseService.addDocument(Constants.TOKENS_COLLECTION, tokenData)
                delay(100) // Small delay to avoid rate limiting
            }

            Result.success("Created ${tokens.size} dummy tokens: ${tokens.joinToString(", ")}")
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

            // Create tokens
            createDummyTokens().onSuccess { results.add(it) }
            delay(1000)

            Result.success("All dummy data created successfully!\n${results.joinToString("\n")}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}