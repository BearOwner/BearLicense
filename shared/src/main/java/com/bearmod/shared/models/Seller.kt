package com.bearmod.shared.models

data class Seller(
    val id: String,
    val username: String,
    val email: String,
    val inviteKey: String,
    val verified: Boolean,
    val banned: Boolean,
    val balance: Double
)
