package com.duta.lubanagym.data.model

data class User(
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val role: String = "member",
    val createdAt: Long = System.currentTimeMillis(),

    // Extended profile data - bisa diedit di profil
    val fullName: String = "",
    val phone: String = "",
    val dateOfBirth: String = "", // format: DD/MM/YYYY
    val gender: String = "", // male, female
    val address: String = "",
    val profileImageUrl: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val isProfileComplete: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)