package com.ottawa.wastesorter.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.domain.model.CatalogItem
import com.ottawa.wastesorter.domain.model.ItemCatalog
import com.ottawa.wastesorter.rules.ClassificationEngine
import com.ottawa.wastesorter.rules.ClassificationRequest
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.ui.components.BackButton
import com.ottawa.wastesorter.ui.components.CapsLabel
import com.ottawa.wastesorter.ui.components.Divider1
import com.ottawa.wastesorter.ui.components.HistoryChip
import com.ottawa.wastesorter.ui.components.ItemRow
import com.ottawa.wastesorter.ui.components.OwsCard
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.components.TextAction
import com.ottawa.wastesorter.ui.theme.BROWSE_MATERIALS
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import com.ottawa.wastesorter.ui.theme.label
import com.ottawa.wastesorter.ui.theme.look
import kotlinx.coroutines.launch

/**
 * Écrans 07 / 07b / S4 — Recherche manuelle (hors ligne).
 * Les bacs affichés sont calculés par le moteur de règles, jamais écrits en dur.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SearchScreen(
    language: String,
    onBack: () -> Unit,
    onOpenItem: (CatalogItem) -> Unit,
    onMaterial: (name: String, material: Material) -> Unit,
    onTakePhoto: () -> Unit,
) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = null)
    var query by rememberSaveable { mutableStateOf("") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val results = remember(query) { ItemCatalog.search(query) }
    val q = query.trim()

    fun open(item: CatalogItem) {
        if (q.isNotEmpty()) scope.launch { container.settings.addRecentSearch(q) }
        onOpenItem(item)
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding()) {
        // Champ de recherche actif (contour vert 2 dp)
        Row(
            Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp).fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp))
                .background(Ows.Surface).border(2.dp, Ows.Green, RoundedCornerShape(28.dp)).padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BackButton(onBack)
            Box(Modifier.weight(1f)) {
                if (query.isEmpty()) Text(stringResource(R.string.search_placeholder), style = OwsType.Body, color = Ows.Muted)
                BasicTextField(
                    value = query,
                    onValueChange = { query = it.take(60) },
                    singleLine = true,
                    textStyle = OwsType.Body.copy(color = Ows.Charcoal),
                    cursorBrush = SolidColor(Ows.Green),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { results.firstOrNull()?.let(::open) }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focus),
                )
            }
            if (query.isNotEmpty()) {
                Box(Modifier.size(48.dp).clip(CircleShape).clickable(role = Role.Button) { query = "" }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Close, stringResource(R.string.action_clear), tint = Ows.Secondary, modifier = Modifier.size(22.dp))
                }
            }
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            when {
                q.isEmpty() -> {
                    val recents = settings?.recentSearches.orEmpty()
                    if (recents.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            CapsLabel(stringResource(R.string.search_recent))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                recents.forEach { r -> HistoryChip(r, Icons.Rounded.History) { query = r } }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CapsLabel(stringResource(R.string.search_popular))
                        OwsCard {
                            ItemCatalog.popular.forEachIndexed { i, item ->
                                if (i > 0) Divider1()
                                CatalogRow(item, language, compact = true) { open(item) }
                            }
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        CapsLabel(stringResource(R.string.search_by_material))
                        BROWSE_MATERIALS.chunked(2).forEach { pair ->
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                pair.forEach { m ->
                                    val name = stringResource(m.label())
                                    MaterialChip(name, m, Modifier.weight(1f)) { onMaterial(name, m) }
                                }
                            }
                        }
                    }
                }
                results.isNotEmpty() -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        CapsLabel(stringResource(R.string.search_results))
                        OwsCard {
                            results.forEachIndexed { i, item ->
                                if (i > 0) Divider1()
                                CatalogRow(item, language, compact = false) { open(item) }
                            }
                        }
                    }
                }
                else -> NoResults(
                    query = q,
                    onManual = {
                        scope.launch { container.settings.addRecentSearch(q) }
                        onMaterial(q.replaceFirstChar { it.uppercase() }, Material.RIGID_PLASTIC)
                    },
                    onTakePhoto = onTakePhoto,
                )
            }
        }
    }
}

/** Les bacs d'un objet du catalogue, calculés par le moteur (même logique que le résultat). */
private fun CatalogItem.destinations(language: String): List<Destination> =
    ClassificationEngine().classify(ClassificationRequest(name.get(language), objectType, components(language))).destinations

@Composable
private fun CatalogRow(item: CatalogItem, language: String, compact: Boolean, onClick: () -> Unit) {
    val destinations = remember(item.id, language) { item.destinations(language) }
    ItemRow(
        title = item.name.get(language),
        material = item.primaryMaterial,
        destinations = destinations,
        onClick = onClick,
        thumbSize = if (compact) 40.dp else 44.dp,
        showChevron = !compact,
    )
}

@Composable
private fun MaterialChip(text: String, material: Material, modifier: Modifier, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.height(52.dp).clip(shape).background(Ows.Surface).border(1.dp, Ows.Border, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(material.look().icon, null, tint = Ows.Green, modifier = Modifier.size(22.dp))
        Text(text, style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Ows.Charcoal, maxLines = 2)
    }
}

/** État S4 — Aucun résultat. */
@Composable
private fun NoResults(query: String, onManual: () -> Unit, onTakePhoto: () -> Unit) {
    Column(
        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(Modifier.size(72.dp).clip(CircleShape).background(Ows.Divider), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.SearchOff, null, tint = Ows.Secondary, modifier = Modifier.size(34.dp))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.search_none_title), style = OwsType.CardTitle.copy(fontSize = 20.sp), color = Ows.Charcoal, textAlign = TextAlign.Center)
            Text(stringResource(R.string.search_none_text, query), style = OwsType.BodySmall, color = Ows.Secondary, textAlign = TextAlign.Center)
        }
        PrimaryButton(stringResource(R.string.search_choose_material), onManual, icon = Icons.Rounded.Tune)
        TextAction(stringResource(R.string.search_take_photo), onTakePhoto)
    }
}

