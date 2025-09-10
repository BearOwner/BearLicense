package com.bearmod.license.network.repository

import com.bearmod.license.models.User
import com.bearmod.license.models.Role

interface AuthRepository {
    suspend fun login(email: String, password: String): Result<User>
    suspend fun logout(): Result<Unit>
    suspend fun currentUser(): Result<User?>
}
