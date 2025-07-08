// Updated MemberRepository.kt
package com.duta.lubanagym.data.repository

import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.utils.Constants

class MemberRepository(private val firebaseService: FirebaseService) {

    suspend fun createMember(member: Member): Result<String> {
        return try {
            val memberMap = mapOf(
                "userId" to member.userId,
                "name" to member.name,
                "phone" to member.phone,
                "membershipType" to member.membershipType,
                "joinDate" to member.joinDate,
                "expiryDate" to member.expiryDate,
                "isActive" to member.isActive,
                "profileImageUrl" to member.profileImageUrl,
                "qrCode" to member.qrCode,

                // Extended profile data
                "address" to member.address,
                "emergencyContact" to member.emergencyContact,
                "emergencyPhone" to member.emergencyPhone,
                "bloodType" to member.bloodType,
                "allergies" to member.allergies,
                "dateOfBirth" to member.dateOfBirth,
                "gender" to member.gender
            )

            val docRef = firebaseService.addDocument(Constants.MEMBERS_COLLECTION, memberMap)
            Result.success(docRef.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllMembers(): Result<List<Member>> {
        return try {
            val snapshot = firebaseService.getCollection(Constants.MEMBERS_COLLECTION)
            val members = snapshot.documents.mapNotNull { doc ->
                Member(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    membershipType = doc.getString("membershipType") ?: "",
                    joinDate = doc.getLong("joinDate") ?: 0L,
                    expiryDate = doc.getLong("expiryDate") ?: 0L,
                    isActive = doc.getBoolean("isActive") ?: true,
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    qrCode = doc.getString("qrCode") ?: "",

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    bloodType = doc.getString("bloodType") ?: "",
                    allergies = doc.getString("allergies") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: ""
                )
            }
            Result.success(members)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getMemberByUserId(userId: String): Result<Member?> {
        return try {
            val snapshot = firebaseService.getCollectionWhere(Constants.MEMBERS_COLLECTION, "userId", userId)
            if (snapshot.documents.isEmpty()) {
                Result.success(null)
            } else {
                val doc = snapshot.documents[0]
                val member = Member(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    membershipType = doc.getString("membershipType") ?: "",
                    joinDate = doc.getLong("joinDate") ?: 0L,
                    expiryDate = doc.getLong("expiryDate") ?: 0L,
                    isActive = doc.getBoolean("isActive") ?: true,
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    qrCode = doc.getString("qrCode") ?: "",

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    bloodType = doc.getString("bloodType") ?: "",
                    allergies = doc.getString("allergies") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: ""
                )
                Result.success(member)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateMember(memberId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            firebaseService.updateDocument(Constants.MEMBERS_COLLECTION, memberId, updates)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMember(memberId: String): Result<Unit> {
        return try {
            firebaseService.deleteDocument(Constants.MEMBERS_COLLECTION, memberId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteMemberByUserId(userId: String): Result<Unit> {
        return try {
            val memberResult = getMemberByUserId(userId)
            memberResult.onSuccess { member ->
                if (member != null) {
                    return deleteMember(member.id)
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

    suspend fun getMemberById(memberId: String): Result<Member?> {
        return try {
            val doc = firebaseService.getDocument(Constants.MEMBERS_COLLECTION, memberId)
            if (doc.exists()) {
                val member = Member(
                    id = doc.id,
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "",
                    phone = doc.getString("phone") ?: "",
                    membershipType = doc.getString("membershipType") ?: "",
                    joinDate = doc.getLong("joinDate") ?: 0L,
                    expiryDate = doc.getLong("expiryDate") ?: 0L,
                    isActive = doc.getBoolean("isActive") ?: true,
                    profileImageUrl = doc.getString("profileImageUrl") ?: "",
                    qrCode = doc.getString("qrCode") ?: "",

                    // Extended profile data
                    address = doc.getString("address") ?: "",
                    emergencyContact = doc.getString("emergencyContact") ?: "",
                    emergencyPhone = doc.getString("emergencyPhone") ?: "",
                    bloodType = doc.getString("bloodType") ?: "",
                    allergies = doc.getString("allergies") ?: "",
                    dateOfBirth = doc.getString("dateOfBirth") ?: "",
                    gender = doc.getString("gender") ?: ""
                )
                Result.success(member)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}