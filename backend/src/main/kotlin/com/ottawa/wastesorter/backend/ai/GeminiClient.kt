package com.ottawa.wastesorter.backend.ai

import com.ottawa.wastesorter.rules.api.AiIdentification
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import kotlinx.serialization.json.addJsonObject
import org.slf4j.LoggerFactory
import java.util.Base64

/**
 * Appel REST à l'API Gemini (compréhension d'image) avec sortie JSON structurée.
 * La clé n'est jamais journalisée ; l'image n'est ni journalisée ni conservée.
 */
class GeminiClient(
    private val apiKey: String,
    private val model: String,
    private val http: HttpClient = defaultClient(),
) : ImageIdentifier {

    private val log = LoggerFactory.getLogger(GeminiClient::class.java)

    override suspend fun identify(image: ByteArray, mimeType: String, language: String): AiIdentification {
        val requestBody = buildJsonObject {
            putJsonArray("contents") {
                addJsonObject {
                    put("role", "user")
                    putJsonArray("parts") {
                        addJsonObject { put("text", GeminiPrompt.instructions(language)) }
                        addJsonObject {
                            putJsonObject("inlineData") {
                                put("mimeType", mimeType)
                                put("data", Base64.getEncoder().encodeToString(image))
                            }
                        }
                    }
                }
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                put("responseSchema", GeminiPrompt.responseSchema)
                put("temperature", 0.1)
            }
        }

        val body: JsonObject = try {
            val response = http.post("$BASE_URL/models/$model:generateContent") {
                header("x-goog-api-key", apiKey)
                contentType(ContentType.Application.Json)
                setBody(requestBody)
            }
            if (!response.status.isSuccess()) {
                log.warn("Gemini request failed with HTTP {}", response.status.value)
                throw AiUnavailableException("AI service returned HTTP ${response.status.value}")
            }
            response.body<JsonObject>()
        } catch (e: CancellationException) {
            throw e
        } catch (e: AiUnavailableException) {
            throw e
        } catch (e: Exception) {
            log.warn("Gemini request error: {}", e.javaClass.simpleName)
            throw AiUnavailableException("AI service unreachable")
        }

        val text = try {
            body["candidates"]?.jsonArray?.firstOrNull()?.jsonObject
                ?.get("content")?.jsonObject
                ?.get("parts")?.jsonArray
                ?.mapNotNull { it.jsonObject["text"]?.jsonPrimitive?.contentOrNull }
                ?.joinToString("")
        } catch (e: IllegalArgumentException) {
            null
        }
        if (text.isNullOrBlank()) throw InvalidAiResponseException("AI response has no content")

        return AiResponseValidator.parse(text)
    }

    companion object {
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta"

        fun defaultClient() = HttpClient(CIO) {
            install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
            install(HttpTimeout) {
                requestTimeoutMillis = 45_000
                connectTimeoutMillis = 10_000
            }
            expectSuccess = false
        }
    }
}
