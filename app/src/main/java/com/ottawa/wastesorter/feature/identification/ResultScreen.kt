package com.ottawa.wastesorter.feature.identification

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.domain.model.IdentificationMethod
import com.ottawa.wastesorter.domain.model.SortingOutcome
import com.ottawa.wastesorter.rules.ComponentResult
import com.ottawa.wastesorter.rules.OttawaRules2026
import com.ottawa.wastesorter.ui.components.BackButton
import com.ottawa.wastesorter.ui.components.ConfidenceBars
import com.ottawa.wastesorter.ui.components.InfoNote
import com.ottawa.wastesorter.ui.components.ItemThumb
import com.ottawa.wastesorter.ui.components.OwsCard
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.components.SecondaryButton
import com.ottawa.wastesorter.ui.components.StepNumber
import com.ottawa.wastesorter.ui.components.TextAction
import com.ottawa.wastesorter.ui.components.TopIconButton
import com.ottawa.wastesorter.ui.components.WarningNote
import com.ottawa.wastesorter.ui.components.confidenceLabelRes
import com.ottawa.wastesorter.ui.components.confidenceLevel
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import com.ottawa.wastesorter.ui.theme.label
import com.ottawa.wastesorter.ui.theme.style

/**
 * Écrans 05 (résultat, variante 1c « Bin card ») et 06 (résultat multi-matériaux),
 * avec la feuille de confiance (S9) et le snackbar d'enregistrement (S8).
 */
@Composable
fun ResultScreen(
    vm: IdentificationViewModel,
    language: String,
    onBack: () -> Unit,
    onIdentifyAnother: () -> Unit,
    onSearch: () -> Unit,
    onViewHistory: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val outcome = state.outcome
    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    var showConfidence by rememberSaveable { mutableStateOf(false) }
    val savedMessage = stringResource(R.string.toast_saved)
    val viewLabel = stringResource(R.string.toast_view)
    var lastShownTick by remember { mutableIntStateOf(state.savedTick) }

    LaunchedEffect(state.savedTick) {
        if (state.savedTick > lastShownTick) {
            lastShownTick = state.savedTick
            val result = snackbar.showSnackbar(savedMessage, actionLabel = viewLabel, duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) onViewHistory()
        }
    }

    Box(Modifier.fillMaxSize()) {
        if (outcome == null) {
            Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.result_none), style = OwsType.Body, color = Ows.Secondary)
            }
        } else {
            Column(
                Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
                    BackButton(onBack)
                    Box(Modifier.weight(1f))
                    TopIconButton(Icons.Rounded.Share, stringResource(R.string.action_share), { share(context, outcome, language) })
                }
                Headline(state.source)
                if (outcome.isMultiPart) {
                    MultiContent(outcome, language)
                } else {
                    SingleContent(outcome, language, photoUri = state.photoUri)
                }
                if (outcome.classification.requiresVerification) VerificationNote()

                val showConf = outcome.method == IdentificationMethod.AI && outcome.confidence != null
                if (showConf) ConfidenceRow(confidenceLevel(outcome.confidence)) { showConfidence = true }

                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (state.saved) {
                        PrimaryButton(
                            stringResource(R.string.result_saved), {}, Modifier.fillMaxWidth(),
                            icon = Icons.Rounded.Bookmark, container = Ows.GreenSoft, content = Ows.GreenDark,
                        )
                    } else {
                        PrimaryButton(stringResource(R.string.result_save), { vm.save() }, Modifier.fillMaxWidth(), icon = Icons.Rounded.BookmarkBorder)
                    }
                    SecondaryButton(stringResource(R.string.result_another), onIdentifyAnother, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
                }
                Text(
                    stringResource(R.string.result_footer, outcome.classification.rulesVersion),
                    style = OwsType.Meta.copy(fontSize = 12.sp), color = Ows.Muted, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                )
            }
        }

        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(16.dp)) { data ->
            SavedSnackbar(data.visuals.message, data.visuals.actionLabel) { data.performAction() }
        }
    }

    if (showConfidence && outcome != null) {
        ConfidenceSheet(
            level = confidenceLevel(outcome.confidence),
            onDismiss = { showConfidence = false },
            onSearch = { showConfidence = false; onSearch() },
        )
    }
}

@Composable
private fun Headline(source: ResultSource) {
    val icon = if (source == ResultSource.HISTORY) Icons.Rounded.History else Icons.Rounded.CheckCircle
    val text = when (source) {
        ResultSource.PHOTO -> R.string.result_head_photo
        ResultSource.HISTORY -> R.string.result_head_history
        ResultSource.MANUAL -> R.string.result_head_manual
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = Ows.Green, modifier = Modifier.size(22.dp))
        Text(stringResource(text), style = OwsType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ows.Green)
    }
}

/** Résumé compact de l'objet, puis grande carte du bac portant les étapes de préparation. */
@Composable
private fun SingleContent(outcome: SortingOutcome, language: String, photoUri: Uri?) {
    val c = outcome.classification.components.first()
    OwsCard(Modifier.fillMaxWidth(), radius = 24.dp, padding = PaddingValues(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ItemThumb(c.material, 104.dp, 18.dp, 48.dp, photoPath = outcome.thumbnailPath, photoUri = photoUri)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(outcome.itemName, style = OwsType.CardTitle, color = Ows.Charcoal, modifier = Modifier.semantics { heading() })
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FactRow(stringResource(R.string.result_material), stringResource(c.material.label()))
                    FactRow(stringResource(R.string.result_condition), stringResource(c.condition.label()))
                }
            }
        }
    }
    BinCard(c, language)
    InfoNote(whyText(c, language))
}

@Composable
private fun FactRow(label: String, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = OwsType.Meta.copy(fontSize = 14.sp), color = Ows.Muted, modifier = Modifier.width(72.dp))
        Text(value, style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Ows.Charcoal)
    }
}

@Composable
private fun BinCard(c: ComponentResult, language: String) {
    val s = c.destination.style()
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(s.bg)
            .border(1.dp, Ows.Charcoal.copy(alpha = 0.08f), RoundedCornerShape(28.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(stringResource(R.string.result_put_it), style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = s.fg.copy(alpha = 0.85f))
                Text(stringResource(s.name).uppercase(), style = OwsType.BinLabel, color = s.fg, modifier = Modifier.semantics { heading() })
                Text(stringResource(s.sub), style = OwsType.Body.copy(fontWeight = FontWeight.Medium), color = s.fg.copy(alpha = 0.9f))
            }
            Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp)).background(s.chip), contentAlignment = Alignment.Center) {
                Icon(s.icon, null, tint = s.fg, modifier = Modifier.size(42.dp))
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(s.fg.copy(alpha = 0.18f)))
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.result_before).uppercase(), style = OwsType.Label, color = s.fg.copy(alpha = 0.8f))
            c.instructions.forEachIndexed { i, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StepNumber(i + 1, s.chip, s.fg)
                    Text(step.get(language), style = OwsType.BodySmall, color = s.fg)
                }
            }
        }
    }
}

/** Explication de la règle : restrictions officielles, sinon une phrase générée. */
@Composable
private fun whyText(c: ComponentResult, language: String): String {
    val notes = c.notes.joinToString(" ") { it.get(language) }
    if (notes.isNotBlank()) return notes
    val rule = OttawaRules2026.rules.firstOrNull { it.id == c.ruleId }
    return if (rule != null) {
        stringResource(R.string.result_why_rule, rule.examples.get(language))
    } else {
        stringResource(R.string.result_why, stringResource(c.material.label()), stringResource(c.destination.style().name))
    }
}

/** Écran 06 — objet en plusieurs parties : une carte par partie, chacune avec son bac. */
@Composable
private fun MultiContent(outcome: SortingOutcome, language: String) {
    val parts = outcome.classification.components
    val binCount = outcome.classification.destinations.size
    val materials = parts.map { stringResource(it.material.label()) }.distinct()
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        ItemThumb(outcome.primaryMaterial, 72.dp, 18.dp, 36.dp, photoPath = outcome.thumbnailPath)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(outcome.itemName, style = OwsType.CardTitle.copy(fontSize = 24.sp), color = Ows.Charcoal, modifier = Modifier.semantics { heading() })
            Text(materials.joinToString(", "), style = OwsType.Meta.copy(fontSize = 14.sp), color = Ows.Secondary)
        }
    }
    WarningNote(
        title = stringResource(R.string.multi_title),
        text = stringResource(R.string.multi_text, parts.size, binCount),
        icon = Icons.AutoMirrored.Rounded.CallSplit,
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        parts.forEachIndexed { i, part -> PartCard(i + 1, part, language) }
    }
    val notes = parts.flatMap { it.notes }.map { it.get(language) }.distinct()
    if (notes.isNotEmpty()) InfoNote(notes.joinToString(" "))
}

@Composable
private fun PartCard(n: Int, part: ComponentResult, language: String) {
    val s = part.destination.style()
    val detail = "${stringResource(part.material.label())} · ${stringResource(part.condition.label())}"
    OwsCard(Modifier.fillMaxWidth(), radius = 22.dp, padding = PaddingValues(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StepNumber(n, Ows.Charcoal, Color.White, size = 28.dp)
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(part.componentName, style = OwsType.Body.copy(fontWeight = FontWeight.Bold), color = Ows.Charcoal)
                Text(detail, style = OwsType.Meta.copy(fontWeight = FontWeight.SemiBold), color = Ows.Secondary)
                Text(part.instructions.joinToString(" ") { it.get(language) }, style = OwsType.Meta, color = Ows.Secondary)
            }
        }
        Row(
            Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(s.bg)
                .border(1.dp, Ows.Charcoal.copy(alpha = 0.08f), RoundedCornerShape(14.dp)).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(s.icon, null, tint = s.fg, modifier = Modifier.size(26.dp))
            Column {
                Text(stringResource(s.name).uppercase(), style = OwsType.BodySmall.copy(fontWeight = FontWeight.ExtraBold), color = s.fg)
                Text(stringResource(s.sub), style = OwsType.Meta.copy(fontSize = 12.sp), color = s.fg.copy(alpha = 0.9f))
            }
        }
    }
}

@Composable
private fun VerificationNote() {
    val uri = LocalUriHandler.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        WarningNote(stringResource(R.string.verify_title), stringResource(R.string.result_verify_notice), Icons.Rounded.Warning)
        TextAction(stringResource(R.string.result_check_city), { uri.openUri(OttawaRules2026.SRC_CITY) })
    }
}

@Composable
private fun ConfidenceRow(level: Int, onClick: () -> Unit) {
    OwsCard(Modifier.fillMaxWidth(), onClick = onClick, padding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(R.string.conf_question), style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Ows.Charcoal, modifier = Modifier.weight(1f))
            ConfidenceBars(level)
            Text(stringResource(confidenceLabelRes(level)), style = OwsType.Meta.copy(fontWeight = FontWeight.Bold), color = Ows.Secondary)
            Icon(Icons.Rounded.ChevronRight, null, tint = Ows.Muted, modifier = Modifier.size(20.dp))
        }
    }
}

/** État S9 — explique ce que signifie la confiance (ce n'est pas un verdict officiel). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ConfidenceSheet(level: Int, onDismiss: () -> Unit, onSearch: () -> Unit) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true), containerColor = Ows.Surface) {
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp).navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text(stringResource(R.string.conf_question), style = OwsType.CardTitle, color = Ows.Charcoal)
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Ows.Background).padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                ConfidenceBars(level, barWidth = 8.dp, barHeight = 20.dp)
                Column {
                    Text(stringResource(confidenceLabelRes(level)), style = OwsType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ows.Charcoal)
                    val desc = when (level) {
                        3 -> R.string.conf_high_desc
                        2 -> R.string.conf_likely_desc
                        else -> R.string.conf_possible_desc
                    }
                    Text(stringResource(desc), style = OwsType.Meta, color = Ows.Secondary)
                }
            }
            Text(stringResource(R.string.conf_explain), style = OwsType.BodySmall, color = Ows.Charcoal)
            Text(stringResource(R.string.conf_hint), style = OwsType.Meta.copy(fontSize = 14.sp, lineHeight = 21.sp), color = Ows.Secondary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.conf_search), onSearch, Modifier.weight(1f))
                PrimaryButton(stringResource(R.string.conf_ok), onDismiss, Modifier.weight(1f))
            }
        }
    }
}

/** Snackbar S8 : fond charbon, coche menthe, action « Voir ». */
@Composable
private fun SavedSnackbar(message: String, action: String?, onAction: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().widthIn(max = 560.dp).heightIn(min = 52.dp).clip(RoundedCornerShape(14.dp)).background(Ows.Charcoal)
            .padding(start = 16.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(Icons.Rounded.CheckCircle, null, tint = Ows.Mint, modifier = Modifier.size(20.dp))
        Text(message, style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium), color = Color.White, modifier = Modifier.weight(1f))
        if (action != null) {
            Box(
                Modifier.height(40.dp).clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button, onClick = onAction).padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) { Text(action, style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = Ows.Mint) }
        }
    }
}

/** Partage en texte : objet, bac(s) et étapes. */
private fun share(context: Context, outcome: SortingOutcome, language: String) {
    val text = buildString {
        append(outcome.itemName).append('\n')
        outcome.classification.components.forEach { c ->
            val bin = context.getString(c.destination.style().name)
            if (outcome.isMultiPart) append("• ").append(c.componentName).append(" → ")
            append(bin).append('\n')
            c.instructions.forEach { append("  – ").append(it.get(language)).append('\n') }
        }
        append('\n').append(context.getString(R.string.share_footer))
    }
    val intent = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    context.startActivity(Intent.createChooser(intent, null).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
