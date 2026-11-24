package com.ottawa.wastesorter.backend.ai

import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.api.AiIdentification
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Vérifie chaque champ de la réponse de l'IA avant qu'elle n'atteigne le moteur de règles.
 * Une réponse hors schéma est rejetée : on ne « répare » pas une réponse douteuse.
 */
object AiResponseValidator {

    private const val MAX_NAME = 120
    private const val MAX_COMPONENTS = 8

    private val json = Json { ignoreUnknownKeys = true }

    fun parse(rawText: String): AiIdentification {
        val cleaned = rawText.trim()
            .removePrefix("```json").removePrefix("```")
            .removeSuffix("```")
            .trim()
        val parsed = try {
            json.decodeFromString(AiIdentification.serializer(), cleaned)
        } catch (e: SerializationException) {
            throw InvalidAiResponseException("AI response does not match the schema")
        } catch (e: IllegalArgumentException) {
            throw InvalidAiResponseException("AI response does not match the schema")
        }
        return validate(parsed)
    }

    fun validate(ai: AiIdentification): AiIdentification {
        if (ai.objectName.isBlank()) throw InvalidAiResponseException("objectName is empty")
        if (ai.confidence.isNaN() || ai.confidence !in 0.0..1.0) {
            throw InvalidAiResponseException("confidence must be between 0 and 1")
        }
        if (ai.components.size > MAX_COMPONENTS) throw InvalidAiResponseException("too many components")
        ai.components.forEach {
            if (it.name.isBlank()) throw InvalidAiResponseException("component name is empty")
        }

        val components = ai.components.ifEmpty {
            // Aucun composant listé : on retombe sur le matériau principal.
            listOf(
                ItemComponent(
                    name = ai.objectName,
                    material = ai.primaryMaterial,
                    condition = if (ai.visibleContamination) Condition.FOOD_RESIDUE else Condition.UNKNOWN,
                ),
            )
        }

        return ai.copy(
            objectName = ai.objectName.trim().take(MAX_NAME),
            components = components.map { it.copy(name = it.name.trim().take(MAX_NAME)) },
            clarificationNeeded = ai.clarificationNeeded?.trim()?.takeIf { it.isNotEmpty() }?.take(300),
        )
    }
}
