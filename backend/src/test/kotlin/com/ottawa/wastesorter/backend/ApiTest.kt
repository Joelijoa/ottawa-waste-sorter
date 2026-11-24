package com.ottawa.wastesorter.backend

import com.ottawa.wastesorter.backend.ai.AiResponseValidator
import com.ottawa.wastesorter.backend.ai.ImageIdentifier
import com.ottawa.wastesorter.backend.ai.InvalidAiResponseException
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType
import com.ottawa.wastesorter.rules.api.AiIdentification
import com.ottawa.wastesorter.rules.api.IdentifyResponse
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ApiTest {

    private val config = AppConfig.fromEnv(emptyMap())
    private val json = Json { ignoreUnknownKeys = true }

    /** Fausse IA : reconnaît toujours une boîte de biscuits. */
    private class FakeIdentifier(private val result: AiIdentification) : ImageIdentifier {
        override suspend fun identify(image: ByteArray, mimeType: String, language: String) = result
    }

    private val biscuitBox = AiIdentification(
        objectName = "Biscuit box",
        objectType = ObjectType.FOOD_PACKAGING,
        primaryMaterial = Material.PAPERBOARD,
        components = listOf(
            ItemComponent("Cardboard box", Material.PAPERBOARD, Condition.CLEAN_EMPTY),
            ItemComponent("Plastic wrapper", Material.FLEXIBLE_PLASTIC, Condition.CLEAN_EMPTY),
        ),
        visibleContamination = false,
        confidence = 0.92,
    )

    private val jpegBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte(), 1, 2, 3, 4)

    @Test fun `health reports rules version`() = testApplication {
        application { module(config, null) }
        val response = client.get("/api/v1/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("ottawa-2026"))
    }

    @Test fun `rules endpoint returns the dataset`() = testApplication {
        application { module(config, null) }
        val body = client.get("/api/v1/rules").bodyAsText()
        assertTrue(body.contains("blue.paper-laminate-container"))
    }

    @Test fun `identify without AI configured returns 503`() = testApplication {
        application { module(config, null) }
        val response = client.submitFormWithBinaryData("/api/v1/identify", formData {
            append("image", jpegBytes, Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
            })
        })
        assertEquals(HttpStatusCode.ServiceUnavailable, response.status)
    }

    @Test fun `identify classifies with the rules engine`() = testApplication {
        application { module(config, FakeIdentifier(biscuitBox)) }
        val response = client.submitFormWithBinaryData("/api/v1/identify", formData {
            append("language", "fr")
            append("image", jpegBytes, Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
            })
        })
        assertEquals(HttpStatusCode.OK, response.status)
        val result = json.decodeFromString(IdentifyResponse.serializer(), response.bodyAsText())
        assertEquals(
            listOf(Destination.BLACK_BIN, Destination.BLUE_BIN),
            result.classification.components.map { it.destination },
        )
        assertFalse(result.needsUserInput)
    }

    @Test fun `image with wrong signature is rejected`() = testApplication {
        application { module(config, FakeIdentifier(biscuitBox)) }
        val response = client.submitFormWithBinaryData("/api/v1/identify", formData {
            append("image", "not an image".toByteArray(), Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
            })
        })
        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    @Test fun `missing image is rejected`() = testApplication {
        application { module(config, FakeIdentifier(biscuitBox)) }
        val response = client.submitFormWithBinaryData("/api/v1/identify", formData { append("language", "en") })
        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test fun `low confidence asks the user for input`() = testApplication {
        application { module(config, FakeIdentifier(biscuitBox.copy(confidence = 0.3))) }
        val response = client.submitFormWithBinaryData("/api/v1/identify", formData {
            append("image", jpegBytes, Headers.build {
                append(HttpHeaders.ContentType, "image/jpeg")
                append(HttpHeaders.ContentDisposition, "filename=\"photo.jpg\"")
            })
        })
        val result = json.decodeFromString(IdentifyResponse.serializer(), response.bodyAsText())
        assertTrue(result.needsUserInput)
    }

    // ── Validation des réponses de l'IA ──

    @Test fun `valid AI JSON is parsed`() {
        val ai = AiResponseValidator.parse(
            """```json
            {"objectName":"Glass jar","objectType":"FOOD_PACKAGING","primaryMaterial":"GLASS",
             "components":[{"name":"Jar","material":"GLASS","condition":"CLEAN_EMPTY","separable":true}],
             "visibleContamination":false,"confidence":0.9,"extra":"ignored"}
            ```""",
        )
        assertEquals(Material.GLASS, ai.components.single().material)
    }

    @Test fun `unknown enum value is rejected`() {
        assertFailsWith<InvalidAiResponseException> {
            AiResponseValidator.parse(
                """{"objectName":"X","objectType":"FOOD_PACKAGING","primaryMaterial":"WOOD",
                 "components":[],"visibleContamination":false,"confidence":0.5}""",
            )
        }
    }

    @Test fun `confidence out of range is rejected`() {
        assertFailsWith<InvalidAiResponseException> {
            AiResponseValidator.validate(biscuitBox.copy(confidence = 1.7))
        }
    }

    @Test fun `prose instead of JSON is rejected`() {
        assertFailsWith<InvalidAiResponseException> { AiResponseValidator.parse("This looks like a box.") }
    }

    @Test fun `empty components fall back to primary material`() {
        val ai = AiResponseValidator.validate(biscuitBox.copy(components = emptyList()))
        assertEquals(Material.PAPERBOARD, ai.components.single().material)
    }
}
