package com.ottawa.wastesorter.feature.identification

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Label
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material.icons.rounded.NetworkCheck
import androidx.compose.material.icons.rounded.Opacity
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.domain.model.IdentifyError
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.ui.components.CloseButton
import com.ottawa.wastesorter.ui.components.ItemThumb
import com.ottawa.wastesorter.ui.components.OwsCard
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.components.SecondaryButton
import com.ottawa.wastesorter.ui.components.TextAction
import com.ottawa.wastesorter.ui.components.confidenceLabelRes
import com.ottawa.wastesorter.ui.components.confidenceLevel
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType

/**
 * Écran 04 — Analyse (et ses états : S6 connexion lente, S3 erreur réseau).
 * L'analyse démarre à l'ouverture de l'écran ; la navigation vers le résultat est pilotée par le ViewModel.
 */
@Composable
fun AnalysisScreen(
    vm: IdentificationViewModel,
    onCancel: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onRetake: () -> Unit,
    onRetry: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val error = state.error
    if (error != null && !state.analyzing) {
        ErrorContent(error, onClose = onCancel, onRetry = onRetry, onSearch = onSearch, onSettings = onSettings, onRetake = onRetake)
        return
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(start = 24.dp, end = 24.dp, bottom = 24.dp)) {
        Spacer(Modifier.height(64.dp))
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterVertically),
        ) {
            ScanningPhoto(state.photoUri)
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.analyzing_title), style = OwsType.CardTitle.copy(fontSize = 24.sp), color = Ows.Charcoal, textAlign = TextAlign.Center)
                Text(
                    stringResource(R.string.analyzing_text), style = OwsType.BodySmall, color = Ows.Secondary,
                    textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp),
                )
            }
            Column(Modifier.widthIn(max = 280.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf(R.string.analyzing_step1, R.string.analyzing_step2, R.string.analyzing_step3).forEachIndexed { i, label ->
                    val step = if (state.slow) 1 else state.analysisStep
                    val color = if (i <= step) Ows.Charcoal else Color(0xFF9AA09B)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                            when {
                                i == step -> CircularProgressIndicator(Modifier.size(16.dp), color = Ows.Green, trackColor = Ows.GreenSoft, strokeWidth = 2.dp)
                                i < step -> Icon(Icons.Rounded.CheckCircle, null, tint = color, modifier = Modifier.size(20.dp))
                                else -> Icon(Icons.Rounded.RadioButtonUnchecked, null, tint = color, modifier = Modifier.size(20.dp))
                            }
                        }
                        Text(stringResource(label), style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = color)
                    }
                }
            }
        }
        if (state.slow) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ows.WarningSoft).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Rounded.NetworkCheck, null, tint = Ows.WarningIcon, modifier = Modifier.size(22.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.slow_title), style = OwsType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ows.WarningInk)
                        Text(stringResource(R.string.slow_text), style = OwsType.Meta, color = Ows.WarningInk2)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SmallButton(stringResource(R.string.slow_wait), Color.White, Ows.Charcoal, Modifier.weight(1f)) { vm.keepWaiting() }
                    SmallButton(stringResource(R.string.slow_search), Ows.Charcoal, Color.White, Modifier.weight(1f)) {
                        vm.cancelAnalysis(); onSearch()
                    }
                }
            }
        } else {
            TextAction(stringResource(R.string.action_cancel), onCancel, Modifier.align(Alignment.CenterHorizontally), color = Ows.Secondary)
        }
    }
}

@Composable
private fun SmallButton(text: String, bg: Color, fg: Color, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(bg).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold), color = fg) }
}

/** Photo de 200 dp avec une ligne de balayage verte animée. */
@Composable
private fun ScanningPhoto(photoUri: android.net.Uri?) {
    val transition = rememberInfiniteTransition(label = "scan")
    val progress by transition.animateFloat(
        initialValue = 0.04f, targetValue = 0.92f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "scanLine",
    )
    BoxWithConstraints(Modifier.size(200.dp).clip(RoundedCornerShape(28.dp))) {
        ItemThumb(com.ottawa.wastesorter.rules.Material.UNKNOWN, 200.dp, 28.dp, 64.dp, photoUri = photoUri)
        Box(
            Modifier.offset(y = maxHeight * progress).padding(horizontal = 12.dp).fillMaxWidth().height(3.dp)
                .shadow(12.dp, RoundedCornerShape(2.dp), ambientColor = Ows.Green, spotColor = Ows.Green)
                .clip(RoundedCornerShape(2.dp)).background(Ows.Green),
        )
    }
}

/** États S3 (réseau), serveur non configuré, quota, image illisible. */
@Composable
private fun ErrorContent(
    error: IdentifyError,
    onClose: () -> Unit,
    onRetry: () -> Unit,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onRetake: () -> Unit,
) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Box(Modifier.heightIn(min = 64.dp), contentAlignment = Alignment.CenterStart) { CloseButton(onClose) }
        Column(
            Modifier.weight(1f).fillMaxWidth().padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterVertically),
        ) {
            Box(Modifier.size(96.dp).clip(CircleShape).background(Ows.WarningSoft), contentAlignment = Alignment.Center) {
                Icon(
                    if (error == IdentifyError.InvalidImage) Icons.Rounded.PhotoCamera else Icons.Rounded.WifiOff,
                    null, tint = Ows.WarningIcon, modifier = Modifier.size(44.dp),
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(error.titleRes()), style = OwsType.CardTitle.copy(fontSize = 24.sp, lineHeight = 30.sp), color = Ows.Charcoal, textAlign = TextAlign.Center)
                Text(stringResource(error.messageRes()), style = OwsType.BodySmall, color = Ows.Secondary, textAlign = TextAlign.Center)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (error) {
                IdentifyError.NotConfigured ->
                    PrimaryButton(stringResource(R.string.nav_settings), onSettings, Modifier.fillMaxWidth(), icon = Icons.Rounded.Settings)
                IdentifyError.InvalidImage ->
                    PrimaryButton(stringResource(R.string.unknown_retake), onRetake, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
                else ->
                    PrimaryButton(stringResource(R.string.action_retry), onRetry, Modifier.fillMaxWidth(), icon = Icons.Rounded.Refresh)
            }
            SecondaryButton(stringResource(R.string.action_manual_search), onSearch, Modifier.fillMaxWidth(), icon = Icons.Rounded.Search)
        }
    }
}

/** État S1 — Objet non identifié. */
@Composable
fun UnknownScreen(onClose: () -> Unit, onRetake: () -> Unit, onManual: () -> Unit) {
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Box(Modifier.heightIn(min = 64.dp), contentAlignment = Alignment.CenterStart) { CloseButton(onClose) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Box(Modifier.size(120.dp).clip(RoundedCornerShape(24.dp)).background(Ows.Divider), contentAlignment = Alignment.Center) {
                Icon(Icons.AutoMirrored.Rounded.HelpOutline, null, tint = Ows.Muted, modifier = Modifier.size(48.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.unknown_title), style = OwsType.CardTitle.copy(fontSize = 26.sp, lineHeight = 31.sp), color = Ows.Charcoal)
                Text(stringResource(R.string.unknown_text), style = OwsType.BodySmall, color = Ows.Secondary)
            }
            OwsCard(Modifier.fillMaxWidth(), padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                Text(stringResource(R.string.unknown_tips_title), style = OwsType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ows.Charcoal)
                Tip(Icons.Rounded.WbSunny, stringResource(R.string.unknown_tip_light))
                Tip(Icons.AutoMirrored.Rounded.Label, stringResource(R.string.unknown_tip_label))
                Tip(Icons.Rounded.CropFree, stringResource(R.string.unknown_tip_one))
            }
        }
        Column(Modifier.padding(top = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PrimaryButton(stringResource(R.string.unknown_retake), onRetake, Modifier.fillMaxWidth(), icon = Icons.Rounded.PhotoCamera)
            SecondaryButton(stringResource(R.string.unknown_manual), onManual, Modifier.fillMaxWidth(), icon = Icons.Rounded.Tune)
        }
    }
}

@Composable
private fun Tip(icon: ImageVector, text: String) {
    Row(Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = Ows.Green, modifier = Modifier.size(20.dp))
        Text(text, style = OwsType.Meta.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Ows.Charcoal)
    }
}

/**
 * État S2 — Faible confiance : l'IA a une piste, une précision suffit.
 * La question vient de l'IA (clarificationNeeded) ; sinon, on demande l'état (propre ou souillé),
 * qui est la précision la plus souvent décisive à Ottawa (bac noir/bleu ou bac vert).
 */
@Composable
fun LowConfidenceScreen(
    vm: IdentificationViewModel,
    language: String,
    onClose: () -> Unit,
    onManual: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val outcome = state.outcome ?: return
    val level = confidenceLevel(outcome.confidence)
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Box(Modifier.heightIn(min = 64.dp), contentAlignment = Alignment.CenterStart) { CloseButton(onClose) }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(24.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                ItemThumb(outcome.primaryMaterial, 72.dp, 18.dp, 36.dp, photoUri = state.photoUri)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(outcome.itemName, style = OwsType.CardTitle.copy(fontSize = 20.sp), color = Ows.Charcoal)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(Ows.Warning))
                        Text(stringResource(if (level >= 2) confidenceLabelRes(level) else R.string.conf_possible), style = OwsType.Meta.copy(fontWeight = FontWeight.SemiBold), color = Ows.Secondary)
                    }
                }
            }
            Text(stringResource(R.string.lowconf_title), style = OwsType.CardTitle.copy(fontSize = 24.sp, lineHeight = 30.sp), color = Ows.Charcoal)
            OwsCard(Modifier.fillMaxWidth(), radius = 24.dp, padding = androidx.compose.foundation.layout.PaddingValues(20.dp)) {
                Text(
                    outcome.message?.get(language)?.takeIf { it.isNotBlank() } ?: stringResource(R.string.lowconf_question),
                    style = OwsType.Section.copy(lineHeight = 24.sp), color = Ows.Charcoal,
                )
                Text(stringResource(R.string.lowconf_explain), style = OwsType.Meta.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Ows.Secondary, modifier = Modifier.padding(top = 12.dp))
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    AnswerRow(Icons.Rounded.CheckCircle, Ows.Green, stringResource(R.string.lowconf_clean)) {
                        vm.analyze(language, condition = Condition.CLEAN_EMPTY)
                    }
                    AnswerRow(Icons.Rounded.Opacity, Ows.WarningIcon, stringResource(R.string.lowconf_soiled)) {
                        vm.analyze(language, condition = Condition.FOOD_RESIDUE)
                    }
                    TextAction(stringResource(R.string.lowconf_other), onManual)
                }
            }
            if (state.analyzing) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Ows.Green, strokeWidth = 2.dp)
                    Text(stringResource(R.string.analyzing_title), style = OwsType.BodySmall, color = Ows.Secondary)
                }
            }
            state.error?.let { Text(stringResource(it.messageRes()), style = OwsType.Meta, color = Ows.Danger) }
        }
    }
}

@Composable
private fun AnswerRow(icon: ImageVector, tint: Color, text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).border(1.5.dp, Ows.Outline, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        Text(text, style = OwsType.BodySmall.copy(fontWeight = FontWeight.SemiBold), color = Ows.Charcoal, modifier = Modifier.weight(1f))
        Icon(Icons.Rounded.ChevronRight, null, tint = Ows.Muted)
    }
}

