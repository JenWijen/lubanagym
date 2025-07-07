package com.duta.lubanagym.data.model

data class Token(
    val id: String = "",
    val token: String = "",
    val isUsed: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val usedAt: Long = 0,
    val createdBy: String = ""
)