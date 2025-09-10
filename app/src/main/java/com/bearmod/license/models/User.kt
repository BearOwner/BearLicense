package com.bearmod.license.models

/**
 * Basic user model aligned with Supabase `users` table.
 */

data class User(
    val id: String,
    val username: String,
    val role: Role,
    val email: String?,
    // epoch millis to avoid java.time dependency
    val createdAt: Long? = null,
    val updatedAt: Long? = null
)

/**
 * Application roles for RBAC.
 */
enum class Role {
    OWNER,
    ADMINISTRATOR,
    RESELLER
}
