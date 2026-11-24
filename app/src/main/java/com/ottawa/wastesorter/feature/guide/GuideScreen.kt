package com.ottawa.wastesorter.feature.guide

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.rules.ClassificationEngine
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.OttawaRules2026
import com.ottawa.wastesorter.rules.SortingRule
import com.ottawa.wastesorter.ui.theme.style

private val GUIDE_ORDER = listOf(
    Destination.GARBAGE,
    Destination.BLACK_BIN,
    Destination.BLUE_BIN,
    Destination.GREEN_BIN,
    Destination.SPECIAL_DROP_OFF,
)

/** Guide de tri généré à partir du jeu de règles embarqué : fonctionne sans connexion. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GuideScreen(language: String, modifier: Modifier = Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    val q = ClassificationEngine.normalize(query)
    val rulesByDestination = GUIDE_ORDER.associateWith { destination ->
        OttawaRules2026.rules.filter { rule ->
            rule.destination == destination &&
                (q.isEmpty() || ClassificationEngine.normalize(rule.examples.en + " " + rule.examples.fr).contains(q))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_guide)) }) },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize().testTag("guide_list"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.guide_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(
                    stringResource(R.string.guide_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            items(GUIDE_ORDER) { destination ->
                val rules = rulesByDestination[destination].orEmpty()
                if (q.isEmpty() || rules.isNotEmpty()) {
                    DestinationCard(destination, rules, language, expandedInitially = q.isNotEmpty())
                }
            }
        }
    }
}

@Composable
private fun DestinationCard(destination: Destination, rules: List<SortingRule>, language: String, expandedInitially: Boolean) {
    val style = destination.style()
    var expanded by rememberSaveable(destination, expandedInitially) { mutableStateOf(expandedInitially) }
    Card(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().background(style.color).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(style.onColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(style.icon, contentDescription = null, tint = style.onColor) }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(stringResource(style.label), style = MaterialTheme.typography.titleMedium, color = style.onColor)
                Text(stringResource(style.description), style = MaterialTheme.typography.bodySmall, color = style.onColor)
            }
            Icon(
                if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = null,
                tint = style.onColor,
            )
        }
        if (expanded) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (destination == Destination.GARBAGE) {
                    Text(stringResource(R.string.guide_garbage_general), style = MaterialTheme.typography.bodyMedium)
                }
                rules.forEachIndexed { index, rule ->
                    if (index > 0) HorizontalDivider()
                    Text(rule.examples.get(language), style = MaterialTheme.typography.titleSmall)
                    rule.instructions.forEach { Text("• " + it.get(language), style = MaterialTheme.typography.bodySmall) }
                    rule.restrictions.forEach {
                        Text(it.get(language), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (!rule.verified) {
                        Text(
                            stringResource(R.string.result_component_verify),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        }
    }
}
