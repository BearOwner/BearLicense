package com.bearmod.license.network

/**
 * Placeholder for your server-side API (if any) to manage license CRUD securely.
 */
object ServerApi {
    suspend fun getLicenseStatus(key: String): Result<String> =
        Result.success("valid")
}
