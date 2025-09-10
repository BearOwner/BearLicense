package com.bearmod.license.models

/**
 * Represents pricing options (loosely based on `price` table).
 */

data class Price(
    val id: String,
    val value: Double,
    val durationDays: Int,
    val amount: Int,
    val role: Role
)
