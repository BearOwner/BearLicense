package com.bearmod.shared.models

// Standalone shared models (mirroring app models) so the shared module
// does not depend on :app compile order.

// Roles used by several models
enum class Role {
    OWNER,
    ADMINISTRATOR,
    RESELLER
}

// License (loosely based on `keys_code` table)
data class License(
    val idKeys: String,
    val game: String,
    val userKey: String,
    val durationDays: Int,
    val expiredDate: Long?,
    val maxDevices: Int,
    val devices: Int,
    val status: String,
    val registrator: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

// Basic user model aligned with Supabase `users` table
data class User(
    val id: String,
    val username: String,
    val role: Role,
    val email: String?,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

// Pricing options (loosely based on `price` table)
data class Price(
    val id: String,
    val value: Double,
    val durationDays: Int,
    val amount: Int,
    val role: Role
)

// Library/file record (loosely based on `lib` table)
data class Lib(
    val id: String,
    val file: String,
    val fileType: String?,
    val fileSize: Long?,
    val pass: String?,
    val time: Long?
)

// Referral code entry (loosely based on `referral_code` table)
data class ReferralCode(
    val id: String,
    val code: String,
    val referral: String?,
    val level: Int?,
    val setSaldo: Double?,
    val usedBy: String?,
    val createdBy: String?,
    val createdAt: Long?,
    val updatedAt: Long?,
    val accExpiration: Long?
)
