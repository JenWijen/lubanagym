package com.duta.lubanagym.data.model

data class Member(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val phone: String = "",
    val membershipType: String = "", // basic, premium, vip
    val joinDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = 0,
    val isActive: Boolean = true,
    val profileImageUrl: String = "",
    val qrCode: String = "",

    // Extended profile data from User
    val address: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val dateOfBirth: String = "",
    val gender: String = ""
)