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
    val createdAt: Long = System.currentTimeMillis()
)