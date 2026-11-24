package com.ottawa.wastesorter.backend

import com.ottawa.wastesorter.backend.ai.AiUnavailableException
import com.ottawa.wastesorter.backend.ai.GeminiClient
import com.ottawa.wastesorter.backend.ai.ImageIdentifier
import com.ottawa.wastesorter.backend.ai.InvalidAiResponseException
import com.ottawa.wastesorter.backend.routes.IDENTIFY_RATE_LIMIT
import com.ottawa.wastesorter.backend.routes.apiRoutes
import com.ottawa.wastesorter.backend.routes.respondError
import com.ottawa.wastesorter.backend.services.IdentificationService
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.origin
import io.ktor.server.plugins.ratelimit.RateLimit
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.header
import io.ktor.server.request.path
import io.ktor.server.routing.routing
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import org.slf4j.event.Level
import kotlin.time.Duration.Companion.seconds

fun main() {
    val config = AppConfig.fromEnv()
    embeddedServer(Netty, port = config.port, host = "0.0.0.0") {
        val identifier = config.geminiApiKey?.let { GeminiClient(apiKey = it, model = config.geminiModel) }
        module(config, identifier)
    }.start(wait = true)
}

/**
 * Module Ktor. [identifier] est injectable pour les tests ; null ⇒ IA non configurée (503 sur /identify).
 */
fun Application.module(config: AppConfig, identifier: ImageIdentifier?) {
    val log = LoggerFactory.getLogger("WasteSorter")
    val service = identifier?.let { IdentificationService(it, minConfidence = config.minConfidence) }
    if (service == null) log.warn("GEMINI_API_KEY is not set: /api/v1/identify will answer 503")

    install(ContentNegotiation) {
        json(Json { encodeDefaults = true; ignoreUnknownKeys = true })
    }

    // Journalise méthode, chemin et statut uniquement : jamais le corps (image) ni les en-têtes (clés).
    install(CallLogging) {
        level = Level.INFO
        filter { call -> call.request.path().startsWith("/api") }
    }

    install(RateLimit) {
        register(IDENTIFY_RATE_LIMIT) {
            rateLimiter(limit = config.rateLimitPerMinute, refillPeriod = 60.seconds)
            requestKey { call ->
                call.request.header("X-Forwarded-For")?.substringBefore(',')?.trim()
                    ?: call.request.origin.remoteHost
            }
        }
    }

    install(StatusPages) {
        exception<InvalidAiResponseException> { call, _ ->
            call.respondError(HttpStatusCode.BadGateway, "AI_INVALID_RESPONSE", "The AI returned an invalid response. Try again or select the material manually.")
        }
        exception<AiUnavailableException> { call, _ ->
            call.respondError(HttpStatusCode.ServiceUnavailable, "AI_UNAVAILABLE", "The AI service is temporarily unavailable.")
        }
        exception<Throwable> { call, cause ->
            log.error("Unhandled error: {}", cause.javaClass.simpleName)
            call.respondError(HttpStatusCode.InternalServerError, "INTERNAL_ERROR", "Unexpected server error.")
        }
    }

    routing {
        apiRoutes(config, service)
    }
}
