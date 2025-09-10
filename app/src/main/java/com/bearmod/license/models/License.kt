package com.bearmod.license.models

/**
 * Represents a license entity (loosely based on `keys_code` table).
 */

data class License(
    val idKeys: String,
    val game: String,
    val userKey: String,
    val durationDays: Int,
    // epoch millis for expiration time
    val expiredDate: Long?,
    val maxDevices: Int,
    val devices: Int,
    val status: String,
    val registrator: String,
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)
