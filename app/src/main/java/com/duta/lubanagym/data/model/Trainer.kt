package com.duta.lubanagym.data.model

data class Trainer(
    val id: String = "",
    val name: String = "",
    val userId: String = "",
    val specialization: String = "",
    val experience: String = "",
    val phone: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),

    // Extended profile data from User
    val address: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val bloodType: String = "",
    val allergies: String = ""
)