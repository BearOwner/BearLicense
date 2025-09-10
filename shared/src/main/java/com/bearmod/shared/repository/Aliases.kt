package com.bearmod.shared.repository

import com.bearmod.shared.models.License
import com.bearmod.shared.models.User

// Minimal shared-facing repository contracts so :shared doesn't depend on :app
interface AuthRepository {
    suspend fun login(username: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun me(): Result<User>
}

interface LicenseRepository {
    suspend fun list(): Result<List<License>>
    suspend fun create(license: License): Result<Unit>
    suspend fun revoke(idKeys: String): Result<Unit>
}

// Simple mock implementations for dev usage
class MockAuthRepository : AuthRepository {
    private var current: User? = null
    override suspend fun login(username: String, password: String): Result<User> =
        Result.success(User(id = "u1", username = username, role = com.bearmod.shared.models.Role.ADMINISTRATOR, email = null))
            .also { current = it.getOrNull() }

    override suspend fun logout(): Result<Unit> = Result.success(Unit).also { current = null }
    override suspend fun me(): Result<User> = current?.let { Result.success(it) } ?: Result.failure(IllegalStateException("not logged in"))
}

class MockLicenseRepository : LicenseRepository {
    private val data = mutableListOf<License>()
    override suspend fun list(): Result<List<License>> = Result.success(data.toList())
    override suspend fun create(license: License): Result<Unit> = Result.success(data.add(license)).map { }
    override suspend fun revoke(idKeys: String): Result<Unit> = Result.success(data.removeIf { it.idKeys == idKeys }).map { }
}
