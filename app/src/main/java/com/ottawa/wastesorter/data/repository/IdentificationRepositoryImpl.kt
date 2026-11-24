package com.ottawa.wastesorter.data.repository

import com.ottawa.wastesorter.core.network.ApiProvider
import com.ottawa.wastesorter.domain.model.IdentifyError
import com.ottawa.wastesorter.domain.model.IdentifyException
import com.ottawa.wastesorter.domain.repository.IdentificationRepository
import com.ottawa.wastesorter.domain.repository.SettingsRepository
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.api.ApiError
import com.ottawa.wastesorter.rules.api.ApiFields
import com.ottawa.wastesorter.rules.api.HealthResponse
import com.ottawa.wastesorter.rules.api.IdentifyResponse
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException
import java.io.IOException

class IdentificationRepositoryImpl(
    private val apiProvider: ApiProvider,
    private val settings: SettingsRepository,
) : IdentificationRepository {

    private val text = "text/plain".toMediaType()

    override suspend fun identify(
        jpeg: ByteArray,
        language: String,
        material: Material?,
        condition: Condition?,
    ): IdentifyResponse {
        val api = apiProvider.api(settings.current().backendUrl)
            ?: throw IdentifyException(IdentifyError.NotConfigured)

        val imagePart = MultipartBody.Part.createFormData(
            ApiFields.IMAGE,
            "photo.jpg",
            jpeg.toRequestBody("image/jpeg".toMediaType()),
        )
        return call {
            api.identify(
                image = imagePart,
                language = language.toRequestBody(text),
                material = material?.name?.toRequestBody(text),
                condition = condition?.name?.toRequestBody(text),
            )
        }
    }

    override suspend fun checkHealth(backendUrl: String): HealthResponse {
        val api = apiProvider.api(backendUrl) ?: throw IdentifyException(IdentifyError.NotConfigured)
        return call { api.health() }
    }

    private suspend fun <T> call(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        val code = runCatching {
            e.response()?.errorBody()?.string()?.let { apiProvider.json.decodeFromString(ApiError.serializer(), it).code }
        }.getOrNull()
        throw IdentifyException(
            when {
                e.code() == 429 -> IdentifyError.RateLimited
                code == "AI_NOT_CONFIGURED" -> IdentifyError.NotConfigured
                code == "AI_UNAVAILABLE" || code == "AI_INVALID_RESPONSE" -> IdentifyError.AiUnavailable
                e.code() == 400 || e.code() == 413 || e.code() == 415 -> IdentifyError.InvalidImage
                else -> IdentifyError.Server(code)
            },
        )
    } catch (e: IOException) {
        throw IdentifyException(IdentifyError.Network)
    } catch (e: SerializationException) {
        throw IdentifyException(IdentifyError.Server("BAD_RESPONSE"))
    }
}
