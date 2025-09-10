package com.bearmod.license.models

/**
 * Represents a referral code entry (loosely based on `referral_code` table).
 */

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
