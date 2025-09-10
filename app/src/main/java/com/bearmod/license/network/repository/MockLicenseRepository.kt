package com.bearmod.license.network.repository

import com.bearmod.license.models.License
import kotlinx.coroutines.delay
import java.util.UUID

class MockLicenseRepository : LicenseRepository {
    private val data = mutableListOf<License>()

    override suspend fun listByUser(userId: String): Result<List<License>> {
        delay(150) // simulate network
        return Result.success(data.toList())
    }

    override suspend fun create(license: License): Result<License> {
        delay(150)
        val created = license.copy(idKeys = license.idKeys.ifBlank { UUID.randomUUID().toString() })
        data.add(0, created)
        return Result.success(created)
    }

    override suspend fun revoke(idKeys: String): Result<Unit> {
        delay(100)
        data.removeAll { it.idKeys == idKeys }
        return Result.success(Unit)
    }
}
