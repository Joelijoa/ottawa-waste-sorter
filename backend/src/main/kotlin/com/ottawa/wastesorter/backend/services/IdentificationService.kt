package com.ottawa.wastesorter.backend.services

import com.ottawa.wastesorter.backend.ai.ImageIdentifier
import com.ottawa.wastesorter.rules.ClassificationEngine
import com.ottawa.wastesorter.rules.ClassificationRequest
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.LocalizedText
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.api.IdentifyResponse

/**
 * Orchestration : IA (identification) → précisions de l'utilisateur → moteur de règles (destination).
 */
class IdentificationService(
    private val identifier: ImageIdentifier,
    private val engine: ClassificationEngine = ClassificationEngine(),
    private val minConfidence: Double = 0.6,
) {

    suspend fun identify(
        image: ByteArray,
        mimeType: String,
        language: String,
        manualMaterial: Material? = null,
        manualCondition: Condition? = null,
    ): IdentifyResponse {
        val ai = identifier.identify(image, mimeType, language)

        var components = ai.components
        if (manualMaterial != null) {
            val first = components.firstOrNull() ?: ItemComponent(ai.objectName, manualMaterial)
            components = listOf(first.copy(material = manualMaterial)) + components.drop(1)
        }
        if (manualCondition != null) {
            components = components.map { it.copy(condition = manualCondition) }
        }

        val classification = engine.classify(
            ClassificationRequest(itemName = ai.objectName, objectType = ai.objectType, components = components),
        )

        val lowConfidence = ai.confidence < minConfidence && manualMaterial == null
        val nothingVerified = classification.components.all { it.destination == Destination.NEEDS_VERIFICATION }
        val clarification: String? = ai.clarificationNeeded
        val needsUserInput = lowConfidence || clarification != null || nothingVerified

        val message = when {
            lowConfidence -> LOW_CONFIDENCE
            clarification != null -> LocalizedText(clarification, clarification)
            nothingVerified -> NOTHING_VERIFIED
            else -> null
        }

        return IdentifyResponse(
            identification = ai.copy(components = components),
            classification = classification,
            needsUserInput = needsUserInput,
            message = message,
        )
    }

    companion object {
        val LOW_CONFIDENCE = LocalizedText(
            en = "The item could not be identified reliably. Select its material manually or take another photo.",
            fr = "L'objet n'a pas pu être identifié avec certitude. Choisissez son matériau manuellement ou reprenez une photo.",
        )
        val NOTHING_VERIFIED = LocalizedText(
            en = "No verified rule matches this item. Select its material manually or check the City's Waste Explorer.",
            fr = "Aucune règle vérifiée ne correspond à cet objet. Choisissez son matériau manuellement ou consultez le Waste Explorer de la Ville.",
        )
    }
}
