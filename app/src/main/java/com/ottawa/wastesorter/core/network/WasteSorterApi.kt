package com.ottawa.wastesorter.core.network

import com.ottawa.wastesorter.rules.api.HealthResponse
import com.ottawa.wastesorter.rules.api.IdentifyResponse
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

/** API REST du backend (voir documentation/api.md). L'application n'appelle jamais Gemini directement. */
interface WasteSorterApi {

    @Multipart
    @POST("api/v1/identify")
    suspend fun identify(
        @Part image: MultipartBody.Part,
        @Part("language") language: RequestBody,
        @Part("material") material: RequestBody?,
        @Part("condition") condition: RequestBody?,
    ): IdentifyResponse

    @GET("api/v1/health")
    suspend fun health(): HealthResponse
}

/** Crée (et met en cache) le client pour l'URL de backend configurée dans les Réglages. */
class ApiProvider {

    val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    private val okHttp = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    @Volatile private var cached: Pair<String, WasteSorterApi>? = null

    /** null si aucune URL valide n'est configurée. */
    fun api(baseUrl: String): WasteSorterApi? {
        val normalized = normalize(baseUrl) ?: return null
        cached?.let { (url, api) -> if (url == normalized) return api }
        val api = Retrofit.Builder()
            .baseUrl(normalized)
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(WasteSorterApi::class.java)
        cached = normalized to api
        return api
    }

    companion object {
        fun normalize(url: String): String? {
            val trimmed = url.trim()
            if (!trimmed.startsWith("https://") && !trimmed.startsWith("http://")) return null
            return if (trimmed.endsWith("/")) trimmed else "$trimmed/"
        }
    }
}
