package com.bearmod.shared.models

data class UserControl(
    val userId: String,
    val canLogin: Boolean = true,
    val canCreateKeys: Boolean = true,
    val canResetKeys: Boolean = true,
    val canAddBalance: Boolean = true
)
