package com.ottawa.wastesorter.data.repository

import com.ottawa.wastesorter.core.database.HistoryDao
import com.ottawa.wastesorter.core.database.HistoryEntity
import com.ottawa.wastesorter.domain.model.HistoryItem
import com.ottawa.wastesorter.domain.model.IdentificationMethod
import com.ottawa.wastesorter.domain.model.SortingOutcome
import com.ottawa.wastesorter.domain.repository.HistoryRepository
import com.ottawa.wastesorter.rules.ClassificationResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.File

class HistoryRepositoryImpl(
    private val dao: HistoryDao,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : HistoryRepository {

    override fun observe(query: String): Flow<List<HistoryItem>> {
        val q = query.trim()
        val source = if (q.isEmpty()) dao.observeAll() else dao.search(q)
        return source.map { list -> list.mapNotNull { it.toDomain() } }
    }

    override fun observeRecent(limit: Int): Flow<List<HistoryItem>> =
        dao.observeRecent(limit).map { list -> list.mapNotNull { it.toDomain() } }

    override suspend fun get(id: Long): HistoryItem? = dao.get(id)?.toDomain()

    override suspend fun save(outcome: SortingOutcome): Long = dao.insert(
        HistoryEntity(
            itemName = outcome.itemName,
            materials = outcome.classification.components.joinToString(",") { it.material.name },
            destinations = outcome.classification.components.joinToString(",") { it.destination.name },
            classificationJson = json.encodeToString(ClassificationResult.serializer(), outcome.classification),
            method = outcome.method.name,
            confidence = outcome.confidence,
            thumbnailPath = outcome.thumbnailPath,
            createdAt = System.currentTimeMillis(),
        ),
    )

    override suspend fun delete(id: Long) {
        dao.get(id)?.thumbnailPath?.let { File(it).delete() }
        dao.delete(id)
    }

    override suspend fun clear() {
        dao.allThumbnailPaths().forEach { File(it).delete() }
        dao.clear()
    }

    private fun HistoryEntity.toDomain(): HistoryItem? {
        // Une entrée illisible (version de format antérieure) est ignorée plutôt que de faire planter l'écran.
        val classification = runCatching {
            json.decodeFromString(ClassificationResult.serializer(), classificationJson)
        }.getOrNull() ?: return null
        return HistoryItem(
            id = id,
            itemName = itemName,
            createdAt = createdAt,
            outcome = SortingOutcome(
                itemName = itemName,
                classification = classification,
                method = runCatching { IdentificationMethod.valueOf(method) }.getOrDefault(IdentificationMethod.MANUAL),
                confidence = confidence,
                thumbnailPath = thumbnailPath,
            ),
        )
    }
}
