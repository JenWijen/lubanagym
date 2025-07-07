package com.duta.lubanagym.data.model

data class Equipment(
    val id: String = "",
    val name: String = "",
    val category: String = "",
    val description: String = "",
    val status: String = "available", // available, maintenance, broken
    val imageUrl: String = "", // Sesuai dengan repository
    val instructions: String = "", // TAMBAH field ini - yang menyebabkan error
    val isAvailable: Boolean = true, // Sesuai dengan repository (bukan isActive)
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
