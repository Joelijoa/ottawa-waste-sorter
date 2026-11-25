package com.ottawa.wastesorter.feature.identification

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonChecked
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.ui.components.BackButton
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.components.ScreenTitle
import com.ottawa.wastesorter.ui.components.SectionTitle
import com.ottawa.wastesorter.ui.components.SelectTile
import com.ottawa.wastesorter.ui.components.TextAction
import com.ottawa.wastesorter.ui.theme.CONDITION_ORDER
import com.ottawa.wastesorter.ui.theme.MORE_MATERIALS
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import com.ottawa.wastesorter.ui.theme.PRIMARY_MATERIALS
import com.ottawa.wastesorter.ui.theme.hint
import com.ottawa.wastesorter.ui.theme.label
import com.ottawa.wastesorter.ui.theme.look

/**
 * Écran 08 — Détails manuels : « De quoi est-il fait ? » + « Dans quel état est-il ? ».
 * Les 7 matériaux du design sont affichés d'abord ; « Autres matériaux » donne accès aux catégories
 * indispensables aux règles 2026 (gobelets, tubes spiralés, mousse, cartons de lait…).
 */
@Composable
fun DetailsScreen(vm: IdentificationViewModel, language: String, onBack: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val d = state.detail
    var showMore by rememberSaveable { mutableStateOf(d.material in MORE_MATERIALS) }
    val fallbackName = stringResource(R.string.details_unnamed)

    Column(Modifier.fillMaxSize().safeDrawingPadding()) {
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(Modifier.heightIn(min = 64.dp), contentAlignment = Alignment.CenterStart) { BackButton(onBack) }
            Column(Modifier.padding(top = 0.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ScreenTitle(d.name.ifBlank { fallbackName })
                Text(stringResource(R.string.details_subtitle), style = OwsType.BodySmall, color = Ows.Secondary)
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.details_material_q))
                MaterialGrid(PRIMARY_MATERIALS, d.material) { vm.setDetailMaterial(it) }
                TextAction(
                    stringResource(if (showMore) R.string.details_less else R.string.details_more),
                    { showMore = !showMore },
                )
                if (showMore) MaterialGrid(MORE_MATERIALS, d.material) { vm.setDetailMaterial(it) }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionTitle(stringResource(R.string.details_condition_q))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    CONDITION_ORDER.forEach { c ->
                        val selected = c == d.condition
                        val shape = RoundedCornerShape(16.dp)
                        Row(
                            Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(if (selected) Ows.GreenSoft else Ows.Surface)
                                .border(if (selected) 2.dp else 1.dp, if (selected) Ows.Green else Ows.Border, shape)
                                .clickable(role = Role.RadioButton) { vm.setDetailCondition(c) }.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Icon(
                                if (selected) Icons.Rounded.RadioButtonChecked else Icons.Rounded.RadioButtonUnchecked, null,
                                tint = if (selected) Ows.Green else Ows.Muted, modifier = Modifier.size(24.dp),
                            )
                            Column {
                                Text(stringResource(c.label()), style = OwsType.BodySmall.copy(fontWeight = FontWeight.SemiBold), color = Ows.Charcoal)
                                Text(stringResource(c.hint()), style = OwsType.Meta, color = Ows.Secondary)
                            }
                        }
                    }
                }
            }
        }
        // Bouton collant en bas
        Column(Modifier.fillMaxWidth().background(Ows.Background)) {
            Box(Modifier.fillMaxWidth().heightIn(min = 1.dp).background(Ows.Border))
            PrimaryButton(
                stringResource(R.string.details_find),
                { vm.findBin(language, fallbackName) },
                Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 16.dp),
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward,
            )
        }
    }
}

@Composable
private fun MaterialGrid(materials: List<com.ottawa.wastesorter.rules.Material>, selected: com.ottawa.wastesorter.rules.Material, onSelect: (com.ottawa.wastesorter.rules.Material) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        materials.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { m ->
                    SelectTile(
                        text = stringResource(m.label()),
                        icon = m.look().icon,
                        selected = m == selected,
                        onClick = { onSelect(m) },
                        modifier = Modifier.weight(1f),
                        trailingCheck = Icons.Rounded.CheckCircle,
                    )
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

