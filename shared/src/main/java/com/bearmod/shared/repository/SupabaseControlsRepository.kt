package com.bearmod.shared.repository

import com.bearmod.shared.models.Audience
import com.bearmod.shared.models.FeatureFlag
import com.bearmod.shared.models.UserControl
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class SupabaseControlsRepository(
    private val baseRestUrl: String,
    private val apiKey: String,
    private val json: Json = Json { ignoreUnknownKeys = true }
) : ControlsRepository {

    private val http = HttpClient(Android)

    // region DTOs
    @Serializable
    private data class FeatureFlagDto(
        @SerialName("flag_key") val flagKey: String,
        val enabled: Boolean,
        val audience: String
    )

    @Serializable
    private data class UserControlDto(
        @SerialName("user_id") val userId: String,
        @SerialName("can_login") val canLogin: Boolean,
        @SerialName("can_create_keys") val canCreateKeys: Boolean,
        @SerialName("can_reset_keys") val canResetKeys: Boolean,
        @SerialName("can_add_balance") val canAddBalance: Boolean
    )
    // endregion

    override suspend fun listFlags(): Result<List<FeatureFlag>> = runCatching {
        val url = "$baseRestUrl/feature_flags?select=*"
        val text: String = http.get(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("apikey", apiKey)
            header("Accept-Profile", "public")
        }.body()
        val items: List<FeatureFlagDto> = json.decodeFromString(text)
        items.map { dto ->
            FeatureFlag(
                key = dto.flagKey,
                audience = when (dto.audience) {
                    "ADMINISTRATOR" -> Audience.ADMINISTRATOR
                    "RESELLER" -> Audience.RESELLER
                    else -> Audience.ALL
                },
                enabled = dto.enabled
            )
        }
    }

    override suspend fun upsertFlag(flag: FeatureFlag): Result<Unit> = runCatching {
        val url = "$baseRestUrl/feature_flags"
        val dto = FeatureFlagDto(
            flagKey = flag.key,
            enabled = flag.enabled,
            audience = flag.audience.name
        )
        val payload = json.encodeToString(listOf(dto))
        http.post(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("apikey", apiKey)
            header("Prefer", "resolution=merge-duplicates,return=representation")
            header("Accept-Profile", "public")
            contentType(ContentType.Application.Json)
            setBody(TextContent(payload, ContentType.Application.Json))
        }
        Unit
    }

    override suspend fun listUserControls(): Result<List<UserControl>> = runCatching {
        val url = "$baseRestUrl/user_controls?select=*"
        val text: String = http.get(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("apikey", apiKey)
            header("Accept-Profile", "public")
        }.body()
        val items: List<UserControlDto> = json.decodeFromString(text)
        items.map { it.toModel() }
    }

    override suspend fun upsertUserControl(control: UserControl): Result<Unit> = runCatching {
        val url = "$baseRestUrl/user_controls"
        val dto = control.toDto()
        val payload = json.encodeToString(listOf(dto))
        http.post(url) {
            header(HttpHeaders.Authorization, "Bearer $apiKey")
            header("apikey", apiKey)
            header("Prefer", "resolution=merge-duplicates,return=representation")
            header("Accept-Profile", "public")
            contentType(ContentType.Application.Json)
            setBody(TextContent(payload, ContentType.Application.Json))
        }
        Unit
    }

    private fun UserControlDto.toModel() = UserControl(
        userId = userId,
        canLogin = canLogin,
        canCreateKeys = canCreateKeys,
        canResetKeys = canResetKeys,
        canAddBalance = canAddBalance
    )

    private fun UserControl.toDto() = UserControlDto(
        userId = userId,
        canLogin = canLogin,
        canCreateKeys = canCreateKeys,
        canResetKeys = canResetKeys,
        canAddBalance = canAddBalance
    )
}
