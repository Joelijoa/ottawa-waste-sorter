package com.ottawa.wastesorter.rules.api

import com.ottawa.wastesorter.rules.ClassificationResult
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.LocalizedText
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType
import kotlinx.serialization.Serializable

/**
 * Contrat d'API partagé entre le backend et l'application Android (même module ⇒ jamais désynchronisé).
 */

/** Ce que l'IA a identifié. L'IA ne décide JAMAIS de la destination : c'est le rôle du moteur de règles. */
@Serializable
data class AiIdentification(
    val objectName: String,
    val objectType: ObjectType,
    val primaryMaterial: Material,
    val components: List<ItemComponent>,
    val visibleContamination: Boolean,
    /** 0.0 – 1.0 */
    val confidence: Double,
    /** Information manquante que l'utilisateur doit préciser, sinon null. */
    val clarificationNeeded: String? = null,
)

@Serializable
data class IdentifyResponse(
    val identification: AiIdentification,
    val classification: ClassificationResult,
    /** true ⇒ l'application propose de préciser le matériau manuellement ou de reprendre la photo. */
    val needsUserInput: Boolean,
    val message: LocalizedText? = null,
)

@Serializable
data class ApiError(val code: String, val message: String)

@Serializable
data class HealthResponse(val status: String, val rulesVersion: String, val aiConfigured: Boolean)

object ApiFields {
    const val IMAGE = "image"
    const val LANGUAGE = "language"
    const val MATERIAL = "material"
    const val CONDITION = "condition"
}
