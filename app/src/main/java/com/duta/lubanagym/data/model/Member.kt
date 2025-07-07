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
    val qrCode: String = ""
)