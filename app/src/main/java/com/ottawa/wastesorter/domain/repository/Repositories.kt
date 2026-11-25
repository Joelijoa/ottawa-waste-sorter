package com.ottawa.wastesorter.domain.repository

import com.ottawa.wastesorter.domain.model.AppSettings
import com.ottawa.wastesorter.domain.model.HistoryItem
import com.ottawa.wastesorter.domain.model.SortingOutcome
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.api.HealthResponse
import com.ottawa.wastesorter.rules.api.IdentifyResponse
import kotlinx.coroutines.flow.Flow

interface IdentificationRepository {
    /** Envoie une image (déjà compressée) au backend. Lève IdentifyException en cas d'échec. */
    suspend fun identify(
        jpeg: ByteArray,
        language: String,
        material: Material?,
        condition: Condition?,
    ): IdentifyResponse

    suspend fun checkHealth(backendUrl: String): HealthResponse
}

interface HistoryRepository {
    fun observe(query: String): Flow<List<HistoryItem>>
    fun observeRecent(limit: Int): Flow<List<HistoryItem>>
    suspend fun get(id: Long): HistoryItem?
    suspend fun save(outcome: SortingOutcome): Long
    suspend fun delete(id: Long)
    suspend fun clear()
}

interface SettingsRepository {
    val settings: Flow<AppSettings>
    suspend fun current(): AppSettings
    suspend fun setBackendUrl(url: String)
    suspend fun setSaveThumbnails(enabled: Boolean)
    suspend fun addRecentSearch(query: String)
}
