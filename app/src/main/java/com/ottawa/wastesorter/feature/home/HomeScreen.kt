package com.ottawa.wastesorter.feature.home

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Recycling
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.core.common.rememberIsOnline
import com.ottawa.wastesorter.domain.model.HistoryItem
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.ui.components.BinIconTile
import com.ottawa.wastesorter.ui.components.Divider1
import com.ottawa.wastesorter.ui.components.ItemRow
import com.ottawa.wastesorter.ui.components.OwsCard
import com.ottawa.wastesorter.ui.components.SearchPill
import com.ottawa.wastesorter.ui.components.SectionTitle
import com.ottawa.wastesorter.ui.components.TextAction
import com.ottawa.wastesorter.ui.components.WarningNote
import com.ottawa.wastesorter.ui.theme.CURBSIDE_BINS
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import com.ottawa.wastesorter.ui.theme.style

/** Écran 01 — Accueil, variante 1a « Card-led ». */
@Composable
fun HomeScreen(
    onTakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onSearch: () -> Unit,
    onOpenBin: (Destination) -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    onSeeAllHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recentFlow = remember(context) { context.appContainer.history.observeRecent(2) }
    val recent by recentFlow.collectAsStateWithLifecycle(initialValue = emptyList())
    val online = rememberIsOnline()

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // En-tête : logo + nom, pastille « Ottawa »
        Row(Modifier.fillMaxWidth().height(48.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(Ows.Green), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.Recycling, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Text(
                stringResource(R.string.app_name), style = OwsType.BodySmall.copy(fontWeight = FontWeight.Bold),
                color = Ows.Charcoal, modifier = Modifier.padding(start = 10.dp).weight(1f),
            )
            Row(
                Modifier.height(32.dp).clip(RoundedCornerShape(16.dp)).background(Ows.Surface)
                    .border(1.dp, Ows.Border, RoundedCornerShape(16.dp)).padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Rounded.LocationOn, null, tint = Ows.Secondary, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.home_city), style = OwsType.Meta.copy(fontWeight = FontWeight.SemiBold), color = Ows.Secondary)
            }
        }

        if (!online) {
            WarningNote(
                title = stringResource(R.string.home_offline_title),
                text = stringResource(R.string.home_offline_text),
                icon = Icons.Rounded.CloudOff,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.home_title), style = OwsType.Display, color = Ows.Charcoal)
            Text(stringResource(R.string.home_subtitle), style = OwsType.Body, color = Ows.Secondary)
        }

        CameraCard(online = online, onTakePhoto = onTakePhoto, onUploadPhoto = onUploadPhoto)

        SearchPill(stringResource(R.string.search_placeholder_short), onSearch, icon = Icons.Rounded.Search)

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionTitle(stringResource(R.string.home_categories))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 16.dp),
                modifier = Modifier.bleed(16).fillMaxWidth(),
            ) {
                items(CURBSIDE_BINS) { bin -> CategoryCard(bin, onClick = { onOpenBin(bin) }) }
            }
        }

        if (recent.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionTitle(stringResource(R.string.home_recent), Modifier.weight(1f))
                    TextAction(stringResource(R.string.action_see_all), onSeeAllHistory)
                }
                OwsCard {
                    recent.forEachIndexed { i, item ->
                        if (i > 0) Divider1()
                        ItemRow(
                            title = item.itemName,
                            material = item.outcome.primaryMaterial,
                            destinations = item.outcome.classification.destinations,
                            onClick = { onOpenHistoryItem(item) },
                            photoPath = item.outcome.thumbnailPath,
                        )
                    }
                }
            }
        }
    }
}

/** Carte verte « Que voulez-vous jeter ? » ; grisée hors ligne (état S5). */
@Composable
private fun CameraCard(online: Boolean, onTakePhoto: () -> Unit, onUploadPhoto: () -> Unit) {
    val cardBg = if (online) Ows.Green else Ows.Disabled
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(cardBg).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(18.dp)).background(Color.White.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.PhotoCamera, null, tint = Color.White, modifier = Modifier.size(30.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.home_card_title), style = OwsType.CardTitle, color = Color.White)
            Text(stringResource(R.string.home_card_text), style = OwsType.BodySmall, color = Color.White.copy(alpha = 0.9f))
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val c = if (online) Ows.Green else Ows.Muted
            Row(
                Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .clickable(enabled = online, role = Role.Button, onClick = onTakePhoto),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PhotoCamera, null, tint = c, modifier = Modifier.size(22.dp))
                Text(
                    stringResource(if (online) R.string.home_take_photo else R.string.home_needs_internet),
                    style = OwsType.Button, color = c, modifier = Modifier.padding(start = 8.dp),
                )
            }
            Row(
                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(16.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClick = onUploadPhoto),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Rounded.PhotoLibrary, null, tint = Color.White, modifier = Modifier.size(20.dp))
                Text(
                    stringResource(R.string.home_upload_photo), style = OwsType.Button.copy(fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    color = Color.White, modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(bin: Destination, onClick: () -> Unit) {
    val s = bin.style()
    OwsCard(Modifier.width(136.dp), onClick = onClick, padding = PaddingValues(16.dp)) {
        BinIconTile(bin, size = 48.dp, iconSize = 26.dp, radius = 14.dp)
        Text(
            stringResource(s.name), style = OwsType.BodySmall.copy(fontWeight = FontWeight.Bold, lineHeight = 19.sp),
            color = Ows.Charcoal, modifier = Modifier.padding(top = 14.dp),
        )
        Text(stringResource(s.sub), style = OwsType.Meta, color = Ows.Secondary, modifier = Modifier.padding(top = 2.dp))
    }
}

/** Fait déborder un élément de [dp] de chaque côté (la rangée de catégories va jusqu'aux bords de l'écran). */
fun Modifier.bleed(dp: Int): Modifier = this.layout { measurable, constraints ->
    val extra = (dp * 2).dp.roundToPx()
    val placeable = measurable.measure(constraints.copy(maxWidth = constraints.maxWidth + extra, minWidth = 0))
    layout(constraints.maxWidth, placeable.height) { placeable.place(-extra / 2, 0) }
}
