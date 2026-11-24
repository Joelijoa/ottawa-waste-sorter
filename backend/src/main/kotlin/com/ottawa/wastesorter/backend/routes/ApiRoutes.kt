package com.ottawa.wastesorter.backend.routes

import com.ottawa.wastesorter.backend.AppConfig
import com.ottawa.wastesorter.backend.services.IdentificationService
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.OttawaRules2026
import com.ottawa.wastesorter.rules.api.ApiError
import com.ottawa.wastesorter.rules.api.ApiFields
import com.ottawa.wastesorter.rules.api.HealthResponse
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.PartData
import io.ktor.http.content.forEachPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.plugins.ratelimit.RateLimitName
import io.ktor.server.plugins.ratelimit.rateLimit
import io.ktor.server.request.header
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.utils.io.readRemaining
import kotlinx.io.readByteArray
import kotlinx.coroutines.CancellationException

val IDENTIFY_RATE_LIMIT = RateLimitName("identify")

private val ALLOWED_MIME_TYPES = setOf("image/jpeg", "image/png", "image/webp")

fun Route.apiRoutes(config: AppConfig, service: IdentificationService?) {
    route("/api/v1") {

        get("/health") {
            call.respond(HealthResponse(status = "ok", rulesVersion = OttawaRules2026.VERSION, aiConfigured = service != null))
        }

        get("/rules") {
            call.respond(OttawaRules2026.dataset)
        }

        rateLimit(IDENTIFY_RATE_LIMIT) {
            post("/identify") {
                if (service == null) {
                    return@post call.respondError(HttpStatusCode.ServiceUnavailable, "AI_NOT_CONFIGURED", "AI identification is not configured on this server")
                }

                val declaredLength = call.request.header(HttpHeaders.ContentLength)?.toLongOrNull()
                if (declaredLength != null && declaredLength > config.maxUploadBytes + MULTIPART_OVERHEAD) {
                    return@post call.respondError(HttpStatusCode.PayloadTooLarge, "IMAGE_TOO_LARGE", "Image exceeds the maximum upload size")
                }

                val upload = try {
                    readUpload(call, config.maxUploadBytes)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    return@post call.respondError(HttpStatusCode.BadRequest, "INVALID_UPLOAD", "The upload could not be read")
                }

                val image = upload.image
                val mimeType = upload.mimeType
                when {
                    image == null || image.isEmpty() ->
                        return@post call.respondError(HttpStatusCode.BadRequest, "IMAGE_MISSING", "An image is required")
                    image.size > config.maxUploadBytes ->
                        return@post call.respondError(HttpStatusCode.PayloadTooLarge, "IMAGE_TOO_LARGE", "Image exceeds the maximum upload size")
                    mimeType == null || mimeType !in ALLOWED_MIME_TYPES || !ImageSignature.matches(image, mimeType) ->
                        return@post call.respondError(HttpStatusCode.UnsupportedMediaType, "UNSUPPORTED_IMAGE", "Only JPEG, PNG or WebP images are accepted")
                }

                val result = service.identify(
                    image = image!!,
                    mimeType = mimeType!!,
                    language = upload.language,
                    manualMaterial = upload.material,
                    manualCondition = upload.condition,
                )
                call.respond(result)
            }
        }
    }
}

private const val MULTIPART_OVERHEAD = 64L * 1024

private class Upload(
    var image: ByteArray? = null,
    var mimeType: String? = null,
    var language: String = "en",
    var material: Material? = null,
    var condition: Condition? = null,
)

private suspend fun readUpload(call: ApplicationCall, maxBytes: Long): Upload {
    val upload = Upload()
    val multipart = call.receiveMultipart(formFieldLimit = maxBytes)
    multipart.forEachPart { part ->
        try {
            when (part) {
                is PartData.FileItem -> if (part.name == ApiFields.IMAGE) {
                    upload.mimeType = part.contentType?.let { "${it.contentType}/${it.contentSubtype}" }
                    upload.image = part.provider().readRemaining().readByteArray()
                }
                is PartData.FormItem -> when (part.name) {
                    ApiFields.LANGUAGE -> upload.language = if (part.value.lowercase().startsWith("fr")) "fr" else "en"
                    ApiFields.MATERIAL -> upload.material = Material.entries.firstOrNull { it.name == part.value }
                    ApiFields.CONDITION -> upload.condition = Condition.entries.firstOrNull { it.name == part.value }
                }
                else -> Unit
            }
        } finally {
            part.dispose()
        }
    }
    return upload
}

internal suspend fun ApplicationCall.respondError(status: HttpStatusCode, code: String, message: String) =
    respond(status, ApiError(code, message))

/** Vérifie que le contenu correspond réellement au type annoncé (signature binaire). */
internal object ImageSignature {
    fun matches(bytes: ByteArray, mimeType: String): Boolean = when (mimeType) {
        "image/jpeg" -> bytes.size > 3 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xD8.toByte()
        "image/png" -> bytes.size > 8 && bytes[0] == 0x89.toByte() && bytes[1] == 'P'.code.toByte() &&
            bytes[2] == 'N'.code.toByte() && bytes[3] == 'G'.code.toByte()
        "image/webp" -> bytes.size > 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" &&
            String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP"
        else -> false
    }
}
