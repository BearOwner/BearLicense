package com.bearmod.shared.repository

import com.bearmod.shared.models.Audience
import com.bearmod.shared.models.FeatureFlag
import com.bearmod.shared.models.UserControl

interface ControlsRepository {
    suspend fun listFlags(): Result<List<FeatureFlag>>
    suspend fun upsertFlag(flag: FeatureFlag): Result<Unit>

    suspend fun listUserControls(): Result<List<UserControl>>
    suspend fun upsertUserControl(control: UserControl): Result<Unit>
}

class MockControlsRepository : ControlsRepository {
    private var flags = mutableListOf(
        FeatureFlag(key = "add_users", audience = Audience.ADMINISTRATOR, enabled = true),
        FeatureFlag(key = "delete_key", audience = Audience.ADMINISTRATOR, enabled = true),
        FeatureFlag(key = "reset_key", audience = Audience.ADMINISTRATOR, enabled = true),
        FeatureFlag(key = "add_balance", audience = Audience.RESELLER, enabled = true),
    )
    private var controls = mutableListOf(
        UserControl(userId = "admin-1", canLogin = true, canCreateKeys = true, canResetKeys = true, canAddBalance = false),
        UserControl(userId = "seller-1", canLogin = true, canCreateKeys = false, canResetKeys = false, canAddBalance = true),
    )

    override suspend fun listFlags(): Result<List<FeatureFlag>> = Result.success(flags.toList())
    override suspend fun upsertFlag(flag: FeatureFlag): Result<Unit> = runCatching {
        val idx = flags.indexOfFirst { it.key == flag.key && it.audience == flag.audience }
        if (idx >= 0) flags[idx] = flag else flags += flag
    }

    override suspend fun listUserControls(): Result<List<UserControl>> = Result.success(controls.toList())
    override suspend fun upsertUserControl(control: UserControl): Result<Unit> = runCatching {
        val idx = controls.indexOfFirst { it.userId == control.userId }
        if (idx >= 0) controls[idx] = control else controls += control
    }
}
