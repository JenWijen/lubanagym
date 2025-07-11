package com.duta.lubanagym.data.model

data class Staff (
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val position: String = "",
    val joinDate: Long = System.currentTimeMillis(),
    val isActive: Boolean = true,
    val profileImageUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Extended profile data from User
    val address: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = "",
    val dateOfBirth: String = "",
    val gender: String = ""
)