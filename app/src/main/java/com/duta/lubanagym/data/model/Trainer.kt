package com.duta.lubanagym.data.model

data class Trainer(
    val id: String = "",
    val name: String = "",
    // val userId: String = "", // HAPUS - Trainer tidak terikat dengan User
    val specialization: String = "",
    val experience: String = "",
    val phone: String = "",
    val bio: String = "",
    val profileImageUrl: String = "",
    val isActive: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),

    // Extended profile data (opsional untuk trainer)
    val address: String = "",
    val emergencyContact: String = "",
    val emergencyPhone: String = "",
    val dateOfBirth: String = "",
    val gender: String = "",
    val bloodType: String = "",
    val allergies: String = "",

    // Additional trainer-specific fields
    val certification: String = "", // Sertifikat yang dimiliki
    val hourlyRate: String = "", // Tarif per jam (opsional)
    val availability: String = "", // Jadwal ketersediaan
    val languages: String = "" // Bahasa yang dikuasai
)