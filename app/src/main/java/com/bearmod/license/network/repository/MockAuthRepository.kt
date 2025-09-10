package com.bearmod.license.network.repository

import com.bearmod.license.models.User
import com.bearmod.license.models.Role

class MockAuthRepository : AuthRepository {
    private var mockUser: User? = null

    override suspend fun login(email: String, password: String): Result<User> {
        mockUser = User(
            id = "mock-user-123",
            username = email,
            role = Role.ADMINISTRATOR,
            email = email
        )
        return Result.success(mockUser!!)
    }

    override suspend fun logout(): Result<Unit> {
        mockUser = null
        return Result.success(Unit)
    }

    override suspend fun currentUser(): Result<User?> {
        return Result.success(mockUser)
    }
}
