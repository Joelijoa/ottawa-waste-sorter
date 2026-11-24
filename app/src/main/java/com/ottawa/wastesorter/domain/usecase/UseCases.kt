package com.ottawa.wastesorter.domain.usecase

import com.ottawa.wastesorter.domain.model.IdentificationMethod
import com.ottawa.wastesorter.domain.model.SortingOutcome
import com.ottawa.wastesorter.domain.repository.IdentificationRepository
import com.ottawa.wastesorter.rules.ClassificationEngine
import com.ottawa.wastesorter.rules.ClassificationRequest
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType

/** Tri manuel : 100 % hors ligne, même moteur de règles que pour l'IA. */
class ClassifyManuallyUseCase(private val engine: ClassificationEngine) {
    operator fun invoke(itemName: String, objectType: ObjectType, components: List<ItemComponent>): SortingOutcome {
        val name = itemName.trim().ifEmpty { components.firstOrNull()?.name ?: "Item" }
        val result = engine.classify(ClassificationRequest(name, objectType, components))
        return SortingOutcome(
            itemName = name,
            classification = result,
            method = IdentificationMethod.MANUAL,
            needsUserInput = false,
        )
    }
}

/** Identification par photo : backend (Gemini) puis règles. */
class IdentifyPhotoUseCase(private val repository: IdentificationRepository) {
    suspend operator fun invoke(
        jpeg: ByteArray,
        language: String,
        material: Material? = null,
        condition: Condition? = null,
    ): SortingOutcome {
        val response = repository.identify(jpeg, language, material, condition)
        return SortingOutcome(
            itemName = response.identification.objectName,
            classification = response.classification,
            method = IdentificationMethod.AI,
            confidence = response.identification.confidence,
            needsUserInput = response.needsUserInput,
            message = response.message,
        )
    }
}
