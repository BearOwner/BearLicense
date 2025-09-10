package com.bearmod.license.network.repository

import com.bearmod.license.models.License
import com.bearmod.license.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.content.TextContent
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

interface LicenseRepository {
    suspend fun listByUser(userId: String): Result<List<License>>
    suspend fun create(license: License): Result<License>
    suspend fun revoke(idKeys: String): Result<Unit>
}

class SupabaseLicenseRepository : LicenseRepository {
    private val http = HttpClient(Android)
    private val baseUrl = BuildConfig.SUPABASE_URL.trimEnd('/') + "/rest/v1"
    private val apiKey = BuildConfig.SUPABASE_ANON_KEY
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable
    private data class LicenseDto(
        val id_keys: String? = null,
        val game: String,
        val user_key: String,
        val duration: Int,
        val expired_date: String? = null,
        val max_devices: Int,
        val devices: Int = 0,
        val status: String = "active",
        val registrator: String? = null,
        val created_at: String? = null,
        val updated_at: String? = null
    )

    private fun LicenseDto.toModel(): License =
        License(
            idKeys = id_keys ?: "",
            game = game,
            userKey = user_key,
            durationDays = duration,
            expiredDate = null, // map later if needed
            maxDevices = max_devices,
            devices = devices,
            status = status,
            registrator = registrator ?: "",
            createdAt = null,
            updatedAt = null
        )

    private fun License.toDto(): LicenseDto =
        LicenseDto(
            id_keys = idKeys.takeIf { it.isNotBlank() },
            game = game,
            user_key = userKey,
            duration = durationDays,
            expired_date = null,
            max_devices = maxDevices,
            devices = devices,
            status = status,
            registrator = registrator.ifBlank { null }
        )

    override suspend fun listByUser(userId: String): Result<List<License>> {
        return runCatching {
            val url = "$baseUrl/keys_code?select=*" // filter by registrator later if desired
            val response: HttpResponse = http.get(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header("apikey", apiKey)
                header("Accept-Profile", "public")
            }
            val bodyText: String = response.body()
            val items: List<LicenseDto> = json.decodeFromString(bodyText)
            items.map { it.toModel() }
        }
    }

    override suspend fun create(license: License): Result<License> {
        return runCatching {
            val url = "$baseUrl/keys_code"
            val dto = license.toDto()
            val response: HttpResponse = http.post(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header("apikey", apiKey)
                header("Prefer", "return=representation")
                header("Accept-Profile", "public")
                contentType(ContentType.Application.Json)
                val bodyJson = json.encodeToString(listOf(dto))
                setBody(TextContent(bodyJson, ContentType.Application.Json))
            }
            val bodyText: String = response.body()
            val created: List<LicenseDto> = json.decodeFromString(bodyText)
            created.first().toModel()
        }
    }

    override suspend fun revoke(idKeys: String): Result<Unit> {
        return runCatching {
            val url = "$baseUrl/keys_code?id_keys=eq.$idKeys"
            http.patch(url) {
                header(HttpHeaders.Authorization, "Bearer $apiKey")
                header("apikey", apiKey)
                header("Prefer", "return=representation")
                header("Accept-Profile", "public")
                contentType(ContentType.Application.Json)
                val bodyJson = json.encodeToString(mapOf("status" to "revoked"))
                setBody(TextContent(bodyJson, ContentType.Application.Json))
            }
            Unit
        }
    }
}
