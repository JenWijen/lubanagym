package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.utils.Constants
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.AuthCredential
import java.security.MessageDigest
import java.util.*

class AuthRepository(private val firebaseService: FirebaseService) {

    // UPDATED: Register dengan Firestore saja (tanpa Firebase Auth untuk standar)
    suspend fun register(email: String, password: String, username: String): Result<String> {
        return try {
            // Validasi input
            validateRegistrationInput(email, password, username)

            // Cek apakah email atau username sudah ada
            val existingEmailUser = getUserByEmail(email)
            if (existingEmailUser.isSuccess && existingEmailUser.getOrNull() != null) {
                throw Exception("Email sudah terdaftar")
            }

            val existingUsernameUser = getUserByUsername(username)
            if (existingUsernameUser.isSuccess && existingUsernameUser.getOrNull() != null) {
                throw Exception("Username sudah digunakan")
            }

            // Hash password
            val hashedPassword = hashPassword(password)

            // Generate userId
            val userId = UUID.randomUUID().toString()

            // Create user document
            val user = User(
                id = userId,
                email = email,
                username = username,
                role = Constants.ROLE_GUEST,
                createdAt = System.currentTimeMillis(),
                isProfileComplete = false
            )

            val userMap = mapOf(
                "email" to user.email,
                "username" to user.username,
                "password" to hashedPassword, // Store hashed password
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
                "updatedAt" to user.updatedAt,
                "isEmailVerified" to false,
                "resetToken" to "",
                "resetTokenExpiry" to 0L
            )

            firebaseService.addDocumentWithId(Constants.USERS_COLLECTION, userId, userMap)
            Result.success(userId)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // UPDATED: Login dengan Firestore saja
    suspend fun login(emailOrUsername: String, password: String): Result<User> {
        return try {
            validateLoginInput(emailOrUsername, password)

            // Cari user berdasarkan email atau username
            val user = if (isEmail(emailOrUsername)) {
                getUserByEmail(emailOrUsername).getOrNull()
            } else {
                getUserByUsername(emailOrUsername).getOrNull()
            }

            if (user == null) {
                throw Exception("Email/Username tidak ditemukan")
            }

            // Ambil password hash dari Firestore
            val userDoc = firebaseService.getDocument(Constants.USERS_COLLECTION, user.id)
            val storedPasswordHash = userDoc.getString("password") ?: ""

            if (storedPasswordHash.isEmpty()) {
                throw Exception("Akun ini terdaftar dengan Google Sign-In. Silakan login dengan Google.")
            }

            // Verify password
            if (!verifyPassword(password, storedPasswordHash)) {
                throw Exception("Password salah")
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Forgot Password - Generate reset token
    suspend fun sendPasswordResetToken(email: String): Result<String> {
        return try {
            if (!isValidEmail(email)) {
                throw Exception("Format email tidak valid")
            }

            val user = getUserByEmail(email).getOrNull()
                ?: throw Exception("Email tidak terdaftar")

            // Generate reset token
            val resetToken = generateResetToken()
            val tokenExpiry = System.currentTimeMillis() + (30 * 60 * 1000L) // 30 minutes

            // Update user document with reset token
            val updates = mapOf(
                "resetToken" to resetToken,
                "resetTokenExpiry" to tokenExpiry
            )

            firebaseService.updateDocument(Constants.USERS_COLLECTION, user.id, updates)

            // Return token (in real app, this would be sent via email)
            Result.success(resetToken)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Reset Password with token
    suspend fun resetPassword(email: String, resetToken: String, newPassword: String): Result<String> {
        return try {
            validatePasswordStrength(newPassword)

            val user = getUserByEmail(email).getOrNull()
                ?: throw Exception("Email tidak ditemukan")

            val userDoc = firebaseService.getDocument(Constants.USERS_COLLECTION, user.id)
            val storedToken = userDoc.getString("resetToken") ?: ""
            val tokenExpiry = userDoc.getLong("resetTokenExpiry") ?: 0L

            if (storedToken.isEmpty() || storedToken != resetToken) {
                throw Exception("Token reset tidak valid")
            }

            if (System.currentTimeMillis() > tokenExpiry) {
                throw Exception("Token reset sudah expired")
            }

            // Hash new password
            val hashedPassword = hashPassword(newPassword)

            // Update password and clear reset token
            val updates = mapOf(
                "password" to hashedPassword,
                "resetToken" to "",
                "resetTokenExpiry" to 0L,
                "updatedAt" to System.currentTimeMillis()
            )

            firebaseService.updateDocument(Constants.USERS_COLLECTION, user.id, updates)

            Result.success("Password berhasil direset")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // NEW: Change Password (for logged in users)
    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<String> {
        return try {
            validatePasswordStrength(newPassword)

            val userDoc = firebaseService.getDocument(Constants.USERS_COLLECTION, userId)
            if (!userDoc.exists()) {
                throw Exception("User tidak ditemukan")
            }

            val storedPasswordHash = userDoc.getString("password") ?: ""
            if (storedPasswordHash.isEmpty()) {
                throw Exception("Akun ini menggunakan Google Sign-In, tidak dapat mengubah password")
            }

            if (!verifyPassword(oldPassword, storedPasswordHash)) {
                throw Exception("Password lama salah")
            }

            val hashedNewPassword = hashPassword(newPassword)
            val updates = mapOf(
                "password" to hashedNewPassword,
                "updatedAt" to System.currentTimeMillis()
            )

            firebaseService.updateDocument(Constants.USERS_COLLECTION, userId, updates)

            Result.success("Password berhasil diubah")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Google Sign-In (unchanged)
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
                    "password" to "", // Empty for Google users
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
                    "updatedAt" to newUser.updatedAt,
                    "isEmailVerified" to true, // Google accounts are verified
                    "resetToken" to "",
                    "resetTokenExpiry" to 0L
                )

                firebaseService.addDocumentWithId(Constants.USERS_COLLECTION, userId, userMap)
                newUser
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        // For Google users
        firebaseService.signOut()
    }

    fun getCurrentUser() = firebaseService.getCurrentUser()

    // HELPER METHODS

    private fun validateRegistrationInput(email: String, password: String, username: String) {
        if (!isValidEmail(email)) {
            throw Exception("Format email tidak valid")
        }
        if (!isValidUsername(username)) {
            throw Exception("Username harus terdiri dari huruf saja (a-z, A-Z) minimal 3 karakter")
        }
        validatePasswordStrength(password)
    }

    private fun validateLoginInput(emailOrUsername: String, password: String) {
        if (emailOrUsername.trim().isEmpty()) {
            throw Exception("Email/Username tidak boleh kosong")
        }
        if (password.trim().isEmpty()) {
            throw Exception("Password tidak boleh kosong")
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        val regex = Regex(emailPattern)

        // Email harus mengandung kombinasi huruf dan angka
        val hasLetter = email.any { it.isLetter() }
        val hasDigit = email.any { it.isDigit() }

        return regex.matches(email) && hasLetter && hasDigit && email.length >= 8
    }

    private fun isValidUsername(username: String): Boolean {
        // Username hanya boleh huruf (a-z, A-Z)
        val regex = Regex("^[a-zA-Z]+$")
        return regex.matches(username) && username.length >= 3
    }

    private fun validatePasswordStrength(password: String) {
        if (password.length < 8) {
            throw Exception("Password minimal 8 karakter")
        }

        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }

        if (!hasLetter || !hasDigit) {
            throw Exception("Password harus mengandung kombinasi huruf dan angka")
        }
    }

    private fun isEmail(input: String): Boolean {
        return input.contains("@")
    }

    private fun hashPassword(password: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val salt = "LubanaGymSalt2024" // In production, use random salt per user
        val saltedPassword = password + salt
        val hashedBytes = md.digest(saltedPassword.toByteArray())
        return hashedBytes.joinToString("") { "%02x".format(it) }
    }

    private fun verifyPassword(password: String, hash: String): Boolean {
        return hashPassword(password) == hash
    }

    private fun generateResetToken(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..32)
            .map { chars.random() }
            .joinToString("")
    }

    private suspend fun getUserByEmail(email: String): Result<User?> {
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
                    role = doc.getString("role") ?: Constants.ROLE_GUEST,
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

    private suspend fun getUserByUsername(username: String): Result<User?> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(Constants.USERS_COLLECTION, "username", username)
            if (snapshot.documents.isEmpty()) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val user = User(
                    id = doc.id,
                    email = doc.getString("email") ?: "",
                    username = doc.getString("username") ?: "",
                    role = doc.getString("role") ?: Constants.ROLE_GUEST,
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