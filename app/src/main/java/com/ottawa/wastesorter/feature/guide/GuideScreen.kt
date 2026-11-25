package com.ottawa.wastesorter.feature.guide

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.OttawaRules2026
import com.ottawa.wastesorter.rules.SortingRule
import com.ottawa.wastesorter.ui.components.BackButton
import com.ottawa.wastesorter.ui.components.OwsCard
import com.ottawa.wastesorter.ui.components.ScreenTitle
import com.ottawa.wastesorter.ui.components.StepNumber
import com.ottawa.wastesorter.ui.theme.CURBSIDE_BINS
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import com.ottawa.wastesorter.ui.theme.style

private const val COLLECTION_CALENDAR = "https://ottawa.ca/CollectionCalendar"

/** Règles vérifiées d'un bac, issues du jeu de règles embarqué (fonctionne hors ligne). */
private fun rulesFor(bin: Destination): List<SortingRule> =
    OttawaRules2026.rules.filter { it.destination == bin && it.verified }

@StringRes
private fun descRes(bin: Destination): Int = when (bin) {
    Destination.GARBAGE -> R.string.guide_desc_garbage
    Destination.BLACK_BIN -> R.string.guide_desc_black
    Destination.BLUE_BIN -> R.string.guide_desc_blue
    else -> R.string.guide_desc_green
}

/** Écran 10 — Guide de tri : les quatre flux de collecte en bordure de rue. */
@Composable
fun GuideScreen(language: String, onOpenBin: (Destination) -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ScreenTitle(stringResource(R.string.guide_title))
            Text(stringResource(R.string.guide_subtitle), style = OwsType.BodySmall, color = Ows.Secondary)
        }
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            CURBSIDE_BINS.forEach { bin ->
                val s = bin.style()
                val examples = rulesFor(bin).flatMap { it.examples.get(language).split(", ") }.distinct().take(5)
                OwsCard(Modifier.fillMaxWidth(), radius = 24.dp, onClick = { onOpenBin(bin) }) {
                    Row(
                        Modifier.fillMaxWidth().background(s.bg).padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(s.chip), contentAlignment = Alignment.Center) {
                            Icon(s.icon, null, tint = s.fg, modifier = Modifier.size(30.dp))
                        }
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(s.name), style = OwsType.Section.copy(fontWeight = FontWeight.ExtraBold), color = s.fg)
                            Text(stringResource(s.sub), style = OwsType.Meta.copy(fontSize = 14.sp), color = s.fg.copy(alpha = 0.9f))
                        }
                        Icon(Icons.Rounded.ChevronRight, null, tint = s.fg)
                    }
                    Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(descRes(bin)), style = OwsType.Meta.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Ows.Charcoal)
                        if (bin == Destination.GARBAGE) {
                            Text(stringResource(R.string.guide_garbage_examples), style = OwsType.Meta, color = Ows.Secondary)
                        } else {
                            Text(examples.joinToString(" · "), style = OwsType.Meta, color = Ows.Secondary)
                        }
                    }
                }
            }
        }
        Text(
            stringResource(R.string.guide_footer), style = OwsType.Meta.copy(fontSize = 12.sp), color = Ows.Muted,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Un « non accepté » : l'objet et où il va à la place. */
private data class Rejected(@StringRes val what: Int, @StringRes val where: Int)

private fun rejectedFor(bin: Destination): List<Rejected> = when (bin) {
    Destination.GARBAGE -> listOf(
        Rejected(R.string.rej_batteries, R.string.rej_where_depot),
        Rejected(R.string.rej_chemicals, R.string.rej_where_hhw),
        Rejected(R.string.rej_recyclables, R.string.rej_where_bins),
    )
    Destination.BLACK_BIN -> listOf(
        Rejected(R.string.rej_cups_cans, R.string.rej_where_blue_2026),
        Rejected(R.string.rej_soiled_paper, R.string.rej_where_green),
        Rejected(R.string.rej_plastic_bags, R.string.rej_where_blue),
        Rejected(R.string.rej_books, R.string.rej_where_donate),
    )
    Destination.BLUE_BIN -> listOf(
        Rejected(R.string.rej_batteries, R.string.rej_where_depot),
        Rejected(R.string.rej_aerosols, R.string.rej_where_hhw),
        Rejected(R.string.rej_pots_ceramics, R.string.rej_where_not_recycling),
    )
    else -> listOf(
        Rejected(R.string.rej_diapers, R.string.rej_where_garbage),
        Rejected(R.string.rej_packaging, R.string.rej_where_blue_black),
    )
}

/** Écrans 10b / 10c — détail d'un bac, en-tête pleine largeur à la couleur du bac. */
@Composable
fun GuideDetailScreen(bin: Destination, language: String, onBack: () -> Unit) {
    val s = bin.style()
    val uri = LocalUriHandler.current
    val rules = rulesFor(bin)
    val accepted = rules.map { it.examples.get(language) }.distinct()
    val prep = rules.flatMap { r -> r.instructions.map { it.get(language) } }.distinct().take(4)

    Column(Modifier.fillMaxSize().background(s.bg).verticalScroll(rememberScrollState())) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(Modifier.heightIn(min = 56.dp).offset(x = (-8).dp), contentAlignment = Alignment.CenterStart) { BackButton(onBack, tint = s.fg) }
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(20.dp)).background(s.chip), contentAlignment = Alignment.Center) {
                Icon(s.icon, null, tint = s.fg, modifier = Modifier.size(38.dp))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(s.name), style = OwsType.ScreenTitle.copy(fontWeight = FontWeight.ExtraBold), color = s.fg)
                Text(stringResource(s.sub), style = OwsType.Body, color = s.fg.copy(alpha = 0.9f))
            }
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).clickable(role = Role.Button) { uri.openUri(COLLECTION_CALENDAR) }.padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(Icons.Rounded.Event, null, tint = s.fg, modifier = Modifier.size(18.dp))
                Text(
                    stringResource(R.string.guide_schedule), color = s.fg.copy(alpha = 0.95f),
                    style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline),
                )
            }
        }
        Column(
            Modifier.fillMaxWidth().offset(y = (-24).dp).clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(Ows.Background).navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OwsCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Text(stringResource(R.string.guide_accepted), style = OwsType.Body.copy(fontWeight = FontWeight.Bold), color = Ows.Charcoal, modifier = Modifier.padding(bottom = 6.dp))
                if (bin == Destination.GARBAGE) {
                    Line(Icons.Rounded.Check, Ows.Green, stringResource(R.string.guide_garbage_general), null)
                }
                accepted.forEach { Line(Icons.Rounded.Check, Ows.Green, it, null) }
            }
            OwsCard(Modifier.fillMaxWidth(), padding = PaddingValues(16.dp)) {
                Text(stringResource(R.string.guide_not_accepted), style = OwsType.Body.copy(fontWeight = FontWeight.Bold), color = Ows.Charcoal, modifier = Modifier.padding(bottom = 6.dp))
                rejectedFor(bin).forEach { Line(Icons.Rounded.Close, Ows.Danger, stringResource(it.what), stringResource(it.where)) }
            }
            if (prep.isNotEmpty()) {
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Ows.GreenSoft).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(stringResource(R.string.guide_prepare), style = OwsType.Body.copy(fontWeight = FontWeight.Bold), color = Ows.GreenInk)
                    prep.forEachIndexed { i, text ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            StepNumber(i + 1, Ows.Green, androidx.compose.ui.graphics.Color.White, size = 22.dp)
                            Text(text, style = OwsType.Meta.copy(fontSize = 14.sp, lineHeight = 21.sp), color = Ows.GreenInk)
                        }
                    }
                }
            }
            Text(
                stringResource(R.string.guide_footer), style = OwsType.Meta.copy(fontSize = 12.sp), color = Ows.Muted,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Line(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: androidx.compose.ui.graphics.Color, text: String, sub: String?) {
    Row(Modifier.padding(vertical = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Column {
            Text(text, style = OwsType.BodySmall, color = Ows.Charcoal)
            if (sub != null) Text(sub, style = OwsType.Meta, color = Ows.Secondary)
        }
    }
}

