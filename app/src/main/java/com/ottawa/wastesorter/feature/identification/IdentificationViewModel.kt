package com.ottawa.wastesorter.feature.identification

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ottawa.wastesorter.AppContainer
import com.ottawa.wastesorter.domain.model.CatalogItem
import com.ottawa.wastesorter.domain.model.IdentificationMethod
import com.ottawa.wastesorter.domain.model.IdentifyError
import com.ottawa.wastesorter.domain.model.IdentifyException
import com.ottawa.wastesorter.domain.model.SortingOutcome
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** D'où vient le résultat affiché (change l'en-tête et l'affichage de la confiance). */
enum class ResultSource { PHOTO, MANUAL, HISTORY }

/** Écran à ouvrir après une analyse (événement ponctuel). */
enum class NavEvent { RESULT, LOW_CONFIDENCE, UNKNOWN }

/** Brouillon de l'écran « Détails » (tri manuel). */
data class DetailDraft(
    val name: String,
    val objectType: ObjectType = ObjectType.OTHER,
    val catalog: CatalogItem? = null,
    val material: Material = Material.PAPERBOARD,
    val condition: Condition = Condition.CLEAN_EMPTY,
)

data class IdentificationUiState(
    val photoUri: Uri? = null,
    val analyzing: Boolean = false,
    /** Étape animée de l'analyse : 0 reconnaissance, 1 matériaux, 2 règles d'Ottawa. */
    val analysisStep: Int = 0,
    val slow: Boolean = false,
    val error: IdentifyError? = null,
    val outcome: SortingOutcome? = null,
    val source: ResultSource = ResultSource.PHOTO,
    val saved: Boolean = false,
    /** Incrémenté à chaque enregistrement : déclenche le snackbar. */
    val savedTick: Int = 0,
    val event: NavEvent? = null,
    val detail: DetailDraft = DetailDraft(name = ""),
)

/**
 * Partagé par les écrans du parcours photo, du tri manuel et du résultat (portée : l'activité).
 */
class IdentificationViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(IdentificationUiState())
    val state: StateFlow<IdentificationUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var tickerJob: Job? = null

    /** Dernière analyse réussie de la photo actuelle : évite de payer deux fois le même appel à l'IA. */
    private var lastRequestKey: String? = null
    private var lastAiOutcome: SortingOutcome? = null
    private var lastAiEvent: NavEvent? = null

    // ───────────── Photo ─────────────

    fun setPhoto(uri: Uri) {
        _state.value.photoUri?.let { if (it != uri) container.imageProcessor.deleteIfTemporary(it) }
        resetCache()
        _state.update { it.copy(photoUri = uri, error = null, analyzing = false) }
    }

    fun discardPhoto() {
        cancelAnalysis()
        _state.value.photoUri?.let { container.imageProcessor.deleteIfTemporary(it) }
        resetCache()
        _state.update { it.copy(photoUri = null, error = null) }
    }

    private fun resetCache() {
        lastRequestKey = null
        lastAiOutcome = null
        lastAiEvent = null
    }

    /**
     * Lance l'identification, uniquement après confirmation de l'utilisateur.
     * [material] / [condition] : précisions données par l'utilisateur après une première analyse.
     */
    fun analyze(language: String, material: Material? = null, condition: Condition? = null) {
        val uri = _state.value.photoUri ?: return
        if (_state.value.analyzing) return
        val key = "$uri|$language|$material|$condition"
        val cached = lastAiOutcome
        val cachedEvent = lastAiEvent
        if (key == lastRequestKey && cached != null && cachedEvent != null) {
            _state.update { it.copy(outcome = cached, source = ResultSource.PHOTO, saved = false, error = null, event = cachedEvent) }
            return
        }
        val userAnswered = material != null || condition != null

        tickerJob?.cancel()
        _state.update { it.copy(analyzing = true, analysisStep = 0, slow = false, error = null) }
        tickerJob = viewModelScope.launch {
            delay(900); _state.update { it.copy(analysisStep = 1) }
            delay(900); _state.update { it.copy(analysisStep = 2) }
            delay(SLOW_AFTER_MS - 1_800); _state.update { it.copy(slow = true) }
        }
        job = viewModelScope.launch {
            try {
                val jpeg = withContext(Dispatchers.IO) { container.imageProcessor.prepareUpload(uri) }
                val outcome = container.identifyPhoto(jpeg, language, material, condition)
                val event = when {
                    userAnswered || !outcome.needsUserInput -> NavEvent.RESULT
                    outcome.nothingVerified || (outcome.confidence ?: 1.0) < UNKNOWN_BELOW -> NavEvent.UNKNOWN
                    else -> NavEvent.LOW_CONFIDENCE
                }
                lastRequestKey = key
                lastAiOutcome = outcome
                lastAiEvent = event
                _state.update {
                    it.copy(analyzing = false, outcome = outcome, source = ResultSource.PHOTO, saved = false, event = event)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IdentifyException) {
                _state.update { it.copy(analyzing = false, error = e.error) }
            } catch (e: Exception) {
                // Image illisible ou corrompue.
                _state.update { it.copy(analyzing = false, error = IdentifyError.InvalidImage) }
            } finally {
                tickerJob?.cancel()
            }
        }
    }

    fun keepWaiting() = _state.update { it.copy(slow = false) }

    fun cancelAnalysis() {
        job?.cancel()
        tickerJob?.cancel()
        _state.update { it.copy(analyzing = false, slow = false) }
    }

    fun clearError() = _state.update { it.copy(error = null) }

    fun consumeEvent() = _state.update { it.copy(event = null) }

    // ───────────── Tri manuel ─────────────

    fun startDetails(name: String, material: Material = Material.PAPERBOARD, catalog: CatalogItem? = null) {
        _state.update {
            it.copy(
                detail = DetailDraft(
                    name = name,
                    objectType = catalog?.objectType ?: ObjectType.OTHER,
                    catalog = catalog,
                    material = catalog?.primaryMaterial ?: material,
                    condition = catalog?.parts?.first()?.condition ?: Condition.CLEAN_EMPTY,
                ),
            )
        }
    }

    fun setDetailMaterial(material: Material) = _state.update { it.copy(detail = it.detail.copy(material = material)) }
    fun setDetailCondition(condition: Condition) = _state.update { it.copy(detail = it.detail.copy(condition = condition)) }

    /** « Trouver le bon bac » : objet du catalogue inchangé ⇒ ses composants ; sinon le matériau choisi. */
    fun findBin(language: String, fallbackName: String) {
        val d = _state.value.detail
        val name = d.name.ifBlank { fallbackName }
        val catalog = d.catalog
        val components = if (catalog != null && catalog.primaryMaterial == d.material) {
            catalog.components(language, condition = d.condition.takeIf { !catalog.isMulti })
        } else {
            listOf(ItemComponent(name, d.material, d.condition))
        }
        classifyManually(name, d.objectType, components)
    }

    /** Objet du catalogue à plusieurs parties : résultat direct (comme dans le design). */
    fun classifyCatalog(item: CatalogItem, language: String) =
        classifyManually(item.name.get(language), item.objectType, item.components(language))

    fun classifyManually(itemName: String, objectType: ObjectType, components: List<ItemComponent>) {
        val outcome = container.classifyManually(itemName, objectType, components)
        _state.update { it.copy(outcome = outcome, source = ResultSource.MANUAL, saved = false, error = null, event = NavEvent.RESULT) }
    }

    // ───────────── Résultat ─────────────

    /** Résultat issu de l'historique : il ne correspond pas à la photo en cours, qui est donc abandonnée. */
    fun showOutcome(outcome: SortingOutcome) {
        cancelAnalysis()
        _state.value.photoUri?.let { container.imageProcessor.deleteIfTemporary(it) }
        resetCache()
        _state.update {
            it.copy(photoUri = null, error = null, outcome = outcome, source = ResultSource.HISTORY, saved = true, event = NavEvent.RESULT)
        }
    }

    fun save() {
        val st = _state.value
        val outcome = st.outcome ?: return
        if (st.saved) return
        _state.update { it.copy(saved = true) }
        viewModelScope.launch {
            val photo = st.photoUri
            val thumbnail = if (st.source == ResultSource.PHOTO && photo != null && container.settings.current().saveThumbnails) {
                container.imageProcessor.saveThumbnail(photo)
            } else {
                null
            }
            withContext(Dispatchers.IO) { container.history.save(outcome.copy(thumbnailPath = thumbnail)) }
            _state.update { it.copy(savedTick = it.savedTick + 1) }
        }
    }

    val isAiResult: Boolean get() = _state.value.outcome?.method == IdentificationMethod.AI

    override fun onCleared() {
        _state.value.photoUri?.let { container.imageProcessor.deleteIfTemporary(it) }
    }

    companion object {
        const val SLOW_AFTER_MS = 8_000L
        /** En dessous : « objet non identifié » plutôt que « une précision suffit ». */
        const val UNKNOWN_BELOW = 0.3

        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { IdentificationViewModel(container) }
        }
    }
}
