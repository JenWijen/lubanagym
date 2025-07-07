package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Token
import com.duta.lubanagym.utils.Constants
import java.util.*

class TokenRepository(private val firebaseService: FirebaseService) {

    suspend fun generateToken(createdBy: String): Result<String> {
        return try {
            val tokenString = UUID.randomUUID().toString().take(8).uppercase()
            val token = Token(
                id = "",
                token = tokenString,
                isUsed = false,
                createdAt = System.currentTimeMillis(),
                createdBy = createdBy
            )

            val tokenMap = mapOf(
                "token" to token.token,
                "isUsed" to token.isUsed,
                "createdAt" to token.createdAt,
                "usedAt" to token.usedAt,
                "createdBy" to token.createdBy
            )

            firebaseService.addDocument(Constants.TOKENS_COLLECTION, tokenMap)
            Result.success(tokenString)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTokens(): Result<List<Token>> {
        return try {
            val snapshot = firebaseService.getCollection(Constants.TOKENS_COLLECTION)
            val tokens = snapshot.documents.mapNotNull { doc ->
                Token(
                    id = doc.id,
                    token = doc.getString("token") ?: "",
                    isUsed = doc.getBoolean("isUsed") ?: false,
                    createdAt = doc.getLong("createdAt") ?: 0L,
                    usedAt = doc.getLong("usedAt") ?: 0L,
                    createdBy = doc.getString("createdBy") ?: ""
                )
            }
            Result.success(tokens)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun verifyToken(tokenString: String): Result<Boolean> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(Constants.TOKENS_COLLECTION, "token", tokenString)
            val isValid = snapshot.documents.isNotEmpty() &&
                    snapshot.documents[0].getBoolean("isUsed") == false
            Result.success(isValid)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
