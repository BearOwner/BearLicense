package com.bearmod.shared.repository

import com.bearmod.shared.models.Seller

interface SellersRepository {
    suspend fun list(): Result<List<Seller>>
    suspend fun get(id: String): Result<Seller>
    suspend fun transferBalance(id: String, amount: Double): Result<Double> // returns new balance
    suspend fun setVerified(id: String, verified: Boolean): Result<Unit>
    suspend fun setBanned(id: String, banned: Boolean): Result<Unit>
    suspend fun remove(id: String): Result<Unit>
}

class MockSellersRepository : SellersRepository {
    private val data = mutableListOf(
        Seller("s1", "VenomX", "venom@example.com", "abcd1234", verified = true, banned = false, balance = 150.0),
        Seller("s2", "ThorX", "thor@example.com", "efgh5678", verified = false, banned = false, balance = 40.0),
        Seller("s3", "LokiX", "loki@example.com", "ijkl9012", verified = false, banned = true, balance = 0.0)
    )

    override suspend fun list(): Result<List<Seller>> = Result.success(data.toList())

    override suspend fun get(id: String): Result<Seller> =
        data.find { it.id == id }?.let { Result.success(it) } ?: Result.failure(NoSuchElementException("Seller $id not found"))

    override suspend fun transferBalance(id: String, amount: Double): Result<Double> {
        val idx = data.indexOfFirst { it.id == id }
        if (idx < 0) return Result.failure(NoSuchElementException("Seller $id not found"))
        val s = data[idx]
        val updated = s.copy(balance = s.balance + amount)
        data[idx] = updated
        return Result.success(updated.balance)
    }

    override suspend fun setVerified(id: String, verified: Boolean): Result<Unit> {
        val idx = data.indexOfFirst { it.id == id }
        if (idx < 0) return Result.failure(NoSuchElementException("Seller $id not found"))
        data[idx] = data[idx].copy(verified = verified)
        return Result.success(Unit)
    }

    override suspend fun setBanned(id: String, banned: Boolean): Result<Unit> {
        val idx = data.indexOfFirst { it.id == id }
        if (idx < 0) return Result.failure(NoSuchElementException("Seller $id not found"))
        data[idx] = data[idx].copy(banned = banned)
        return Result.success(Unit)
    }

    override suspend fun remove(id: String): Result<Unit> {
        val removed = data.removeIf { it.id == id }
        return if (removed) Result.success(Unit) else Result.failure(NoSuchElementException("Seller $id not found"))
    }
}
