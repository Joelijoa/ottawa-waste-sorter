package com.ottawa.wastesorter.feature.identification

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.ottawa.wastesorter.AppContainer
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IdentificationUiState(
    val photoUri: Uri? = null,
    val isLoading: Boolean = false,
    val error: IdentifyError? = null,
    val outcome: SortingOutcome? = null,
    /** Événement ponctuel : l'écran de résultat doit s'ouvrir. */
    val showResult: Boolean = false,
)

/**
 * Partagé par les écrans photo, aperçu, tri manuel et résultat (portée : l'activité).
 */
class IdentificationViewModel(private val container: AppContainer) : ViewModel() {

    private val _state = MutableStateFlow(IdentificationUiState())
    val state: StateFlow<IdentificationUiState> = _state.asStateFlow()

    private var job: Job? = null

    /** Clé de la dernière analyse réussie : évite de payer deux fois le même appel à l'IA. */
    private var lastRequestKey: String? = null

    /** Entrée d'historique de la dernière analyse de la photo actuelle (remplacée si l'utilisateur précise le matériau). */
    private var lastSavedId: Long? = null

    fun setPhoto(uri: Uri) {
        _state.value.photoUri?.let { if (it != uri) container.imageProcessor.deleteIfTemporary(it) }
        lastRequestKey = null
        lastSavedId = null
        _state.update { it.copy(photoUri = uri, error = null) }
    }

    fun discardPhoto() {
        job?.cancel()
        _state.value.photoUri?.let { container.imageProcessor.deleteIfTemporary(it) }
        lastRequestKey = null
        lastSavedId = null
        _state.update { it.copy(photoUri = null, isLoading = false, error = null) }
    }

    /** Lance l'identification uniquement après confirmation de l'utilisateur. */
    fun analyze(language: String, material: Material? = null, condition: Condition? = null) {
        val uri = _state.value.photoUri ?: return
        if (_state.value.isLoading) return
        val key = "$uri|$language|$material|$condition"
        if (key == lastRequestKey && _state.value.outcome != null) {
            _state.update { it.copy(showResult = true) }
            return
        }

        job = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val jpeg = withContext(Dispatchers.IO) { container.imageProcessor.prepareUpload(uri) }
                val outcome = container.identifyPhoto(jpeg, language, material, condition)
                val thumbnail = if (container.settings.current().saveThumbnails) {
                    container.imageProcessor.saveThumbnail(uri)
                } else {
                    null
                }
                val saved = outcome.copy(thumbnailPath = thumbnail)
                withContext(Dispatchers.IO) {
                    // Même photo analysée à nouveau (matériau précisé) : on remplace l'entrée au lieu de la dupliquer.
                    lastSavedId?.let { container.history.delete(it) }
                    lastSavedId = container.history.save(saved)
                }
                lastRequestKey = key
                _state.update { it.copy(isLoading = false, outcome = saved, showResult = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: IdentifyException) {
                _state.update { it.copy(isLoading = false, error = e.error) }
            } catch (e: Exception) {
                // Image illisible ou corrompue.
                _state.update { it.copy(isLoading = false, error = IdentifyError.InvalidImage) }
            }
        }
    }

    fun classifyManually(itemName: String, objectType: ObjectType, components: List<ItemComponent>) {
        viewModelScope.launch {
            val outcome = container.classifyManually(itemName, objectType, components)
            withContext(Dispatchers.IO) { container.history.save(outcome) }
            _state.update { it.copy(outcome = outcome, showResult = true, error = null) }
        }
    }

    /** Résultat issu de l'historique : il ne correspond pas à la photo en cours, qui est donc abandonnée. */
    fun showOutcome(outcome: SortingOutcome) {
        job?.cancel()
        _state.value.photoUri?.let { container.imageProcessor.deleteIfTemporary(it) }
        lastRequestKey = null
        lastSavedId = null
        _state.update { it.copy(photoUri = null, isLoading = false, error = null, outcome = outcome, showResult = true) }
    }

    fun onResultShown() = _state.update { it.copy(showResult = false) }

    fun clearError() = _state.update { it.copy(error = null) }

    override fun onCleared() {
        _state.value.photoUri?.let { container.imageProcessor.deleteIfTemporary(it) }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory = viewModelFactory {
            initializer { IdentificationViewModel(container) }
        }
    }
}
