package com.duta.lubanagym.data.model

data class MemberRegistration(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val membershipType: String = "",
    val duration: Int = 0, // dalam bulan
    val price: Int = 0, // dalam rupiah
    val registrationDate: Long = System.currentTimeMillis(),
    val expiryDate: Long = 0, // 5 hari dari registrationDate
    val activationDate: Long = 0, // ketika di-scan oleh admin
    val status: String = "pending", // pending, activated, expired
    val qrCode: String = "",
    val isActive: Boolean = false,
    val activatedBy: String = "", // userId admin/staff yang scan

    // Extended user data for member creation
    val userFullName: String = "",
    val userAddress: String = "",
    val userDateOfBirth: String = "",
    val userGender: String = "",
    val userEmergencyContact: String = "",
    val userEmergencyPhone: String = "",
    val userBloodType: String = "",
    val userAllergies: String = ""
)