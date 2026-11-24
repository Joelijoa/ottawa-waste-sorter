package com.ottawa.wastesorter

import android.app.Application
import android.content.Context
import com.ottawa.wastesorter.core.common.ImageProcessor
import com.ottawa.wastesorter.core.database.AppDatabase
import com.ottawa.wastesorter.core.network.ApiProvider
import com.ottawa.wastesorter.data.local.SettingsDataStore
import com.ottawa.wastesorter.data.repository.HistoryRepositoryImpl
import com.ottawa.wastesorter.data.repository.IdentificationRepositoryImpl
import com.ottawa.wastesorter.domain.repository.HistoryRepository
import com.ottawa.wastesorter.domain.repository.IdentificationRepository
import com.ottawa.wastesorter.domain.repository.SettingsRepository
import com.ottawa.wastesorter.domain.usecase.ClassifyManuallyUseCase
import com.ottawa.wastesorter.domain.usecase.IdentifyPhotoUseCase
import com.ottawa.wastesorter.rules.ClassificationEngine

class WasteSorterApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

/** Injection de dépendances manuelle : suffisante pour le MVP, sans bibliothèque supplémentaire. */
class AppContainer(context: Context) {
    private val database = AppDatabase.create(context)
    private val apiProvider = ApiProvider()

    val engine = ClassificationEngine()
    val settings: SettingsRepository = SettingsDataStore(context)
    val history: HistoryRepository = HistoryRepositoryImpl(database.historyDao())
    val identification: IdentificationRepository = IdentificationRepositoryImpl(apiProvider, settings)
    val imageProcessor = ImageProcessor(context)

    val classifyManually = ClassifyManuallyUseCase(engine)
    val identifyPhoto = IdentifyPhotoUseCase(identification)
}

val Context.appContainer: AppContainer
    get() = (applicationContext as WasteSorterApplication).container
