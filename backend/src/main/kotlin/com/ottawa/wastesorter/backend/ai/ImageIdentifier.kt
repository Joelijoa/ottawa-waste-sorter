package com.ottawa.wastesorter.backend.ai

import com.ottawa.wastesorter.rules.api.AiIdentification

/** Abstraction du fournisseur d'IA : changer de modèle ne touche ni l'application ni les règles. */
interface ImageIdentifier {
    suspend fun identify(image: ByteArray, mimeType: String, language: String): AiIdentification
}

/** Le service d'IA n'a pas répondu correctement (réseau, quota, erreur HTTP). */
class AiUnavailableException(message: String) : RuntimeException(message)

/** L'IA a répondu, mais la réponse ne respecte pas le schéma attendu. */
class InvalidAiResponseException(message: String) : RuntimeException(message)
