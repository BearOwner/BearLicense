package com.bearmod.shared.models

enum class Audience { ALL, ADMINISTRATOR, RESELLER }

data class FeatureFlag(
    val key: String,
    val audience: Audience = Audience.ALL,
    val enabled: Boolean = true
)
