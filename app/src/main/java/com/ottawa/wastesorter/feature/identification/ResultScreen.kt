package com.ottawa.wastesorter.feature.identification

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.domain.model.IdentificationMethod
import com.ottawa.wastesorter.domain.model.SortingOutcome
import com.ottawa.wastesorter.rules.ComponentResult
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.OttawaRules2026
import com.ottawa.wastesorter.ui.theme.MANUAL_MATERIALS
import com.ottawa.wastesorter.ui.theme.label
import com.ottawa.wastesorter.ui.theme.style
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultScreen(
    vm: IdentificationViewModel,
    language: String,
    onManual: () -> Unit,
    onRetake: () -> Unit,
    onDone: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val outcome = state.outcome
    var pickMaterial by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.result_title)) },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        if (outcome == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.result_none))
            }
        } else {
            ResultContent(
                outcome = outcome,
                state = state,
                language = language,
                padding = padding,
                onRefine = { pickMaterial = true },
                onRetake = onRetake,
                onManual = onManual,
                onDone = onDone,
            )
        }
    }

    if (pickMaterial) {
        MaterialPickerDialog(
            onPick = { material ->
                pickMaterial = false
                vm.analyze(language, material = material)
            },
            onDismiss = { pickMaterial = false },
        )
    }
}

@Composable
private fun ResultContent(
    outcome: SortingOutcome,
    state: IdentificationUiState,
    language: String,
    padding: PaddingValues,
    onRefine: () -> Unit,
    onRetake: () -> Unit,
    onManual: () -> Unit,
    onDone: () -> Unit,
) {
        val canRefine = outcome.method == IdentificationMethod.AI && state.photoUri != null

        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Header(outcome) }

            if (outcome.needsUserInput || outcome.classification.requiresVerification) {
                item {
                    Notice(
                        text = outcome.message?.get(language) ?: stringResource(R.string.result_verify_notice),
                        isWarning = true,
                    )
                }
            }

            items(outcome.classification.components) { component ->
                ComponentCard(component, language)
            }

            if (state.isLoading) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                        Spacer(Modifier.size(12.dp))
                        Text(stringResource(R.string.preview_analyzing))
                    }
                }
            }
            state.error?.let { error ->
                item { Notice(stringResource(error.messageRes()), isWarning = true) }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canRefine) {
                        OutlinedButton(onClick = onRefine, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.result_refine))
                        }
                        OutlinedButton(onClick = onRetake, modifier = Modifier.fillMaxWidth()) {
                            Text(stringResource(R.string.preview_retake))
                        }
                    }
                    OutlinedButton(onClick = onManual, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.home_manual))
                    }
                    TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth()) {
                        Text(stringResource(R.string.action_done))
                    }
                }
            }

            item {
                Text(
                    stringResource(R.string.result_rules_version, outcome.classification.rulesVersion),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
}

@Composable
private fun Header(outcome: SortingOutcome) {
    Column {
        Text(
            outcome.itemName,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() },
        )
        val method = if (outcome.method == IdentificationMethod.AI) {
            val confidence = outcome.confidence?.let { (it * 100).roundToInt() }
            if (confidence != null) stringResource(R.string.result_method_ai_confidence, confidence)
            else stringResource(R.string.method_ai)
        } else {
            stringResource(R.string.method_manual)
        }
        Text(method, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ComponentCard(component: ComponentResult, language: String) {
    val style = component.destination.style()
    val uriHandler = LocalUriHandler.current
    Card(Modifier.fillMaxWidth()) {
        Column {
            // Bandeau de destination : couleur + icône + nom (jamais la couleur seule).
            Row(
                Modifier.fillMaxWidth().background(style.color).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(44.dp).clip(CircleShape).background(style.onColor.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(style.icon, contentDescription = null, tint = style.onColor)
                }
                Spacer(Modifier.size(12.dp))
                Column {
                    Text(stringResource(style.label), style = MaterialTheme.typography.titleMedium, color = style.onColor)
                    Text(component.componentName, style = MaterialTheme.typography.bodyMedium, color = style.onColor)
                }
            }
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "${stringResource(component.material.label())} · ${stringResource(component.condition.label())}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                component.instructions.forEach { Text("• " + it.get(language), style = MaterialTheme.typography.bodyMedium) }
                component.notes.forEach {
                    Row {
                        Icon(Icons.Filled.Info, contentDescription = null, modifier = Modifier.size(18.dp).padding(top = 2.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(it.get(language), style = MaterialTheme.typography.bodySmall)
                    }
                }
                if (component.requiresVerification) {
                    Text(
                        stringResource(R.string.result_component_verify),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                FlowRow {
                    component.sourceUrl?.let { url ->
                        TextButton(onClick = { uriHandler.openUri(url) }) { Text(stringResource(R.string.result_source)) }
                    }
                    if (component.requiresVerification) {
                        TextButton(onClick = { uriHandler.openUri(OttawaRules2026.SRC_CITY) }) {
                            Text(stringResource(R.string.result_check_city))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Notice(text: String, isWarning: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.secondaryContainer,
        ),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(if (isWarning) Icons.Filled.Warning else Icons.Filled.Info, contentDescription = null)
            Spacer(Modifier.size(12.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MaterialPickerDialog(onPick: (Material) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.result_refine)) },
        text = {
            FlowRow(
                Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                MANUAL_MATERIALS.forEach { material ->
                    FilterChip(selected = false, onClick = { onPick(material) }, label = { Text(stringResource(material.label())) })
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) } },
    )
}
