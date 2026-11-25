package com.ottawa.wastesorter.domain.model

import com.ottawa.wastesorter.rules.ClassificationResult
import com.ottawa.wastesorter.rules.LocalizedText
import com.ottawa.wastesorter.rules.Material

enum class IdentificationMethod { AI, MANUAL }

/** Résultat affiché à l'utilisateur, qu'il vienne de l'IA ou du tri manuel. */
data class SortingOutcome(
    val itemName: String,
    val classification: ClassificationResult,
    val method: IdentificationMethod,
    val confidence: Double? = null,
    val needsUserInput: Boolean = false,
    val message: LocalizedText? = null,
    val thumbnailPath: String? = null,
) {
    val primaryMaterial: Material get() = classification.components.firstOrNull()?.material ?: Material.UNKNOWN
    val isMultiPart: Boolean get() = classification.components.size > 1
    val nothingVerified: Boolean
        get() = classification.components.all { it.destination == com.ottawa.wastesorter.rules.Destination.NEEDS_VERIFICATION }
}

data class HistoryItem(
    val id: Long,
    val itemName: String,
    val outcome: SortingOutcome,
    val createdAt: Long,
)

data class AppSettings(
    val backendUrl: String,
    val saveThumbnails: Boolean,
    val recentSearches: List<String> = emptyList(),
)

/** Erreurs d'identification présentées à l'utilisateur avec un message clair et une action. */
sealed interface IdentifyError {
    data object NotConfigured : IdentifyError
    data object Network : IdentifyError
    data object RateLimited : IdentifyError
    data object InvalidImage : IdentifyError
    data object AiUnavailable : IdentifyError
    data class Server(val code: String?) : IdentifyError
}

class IdentifyException(val error: IdentifyError) : Exception(error.toString())
