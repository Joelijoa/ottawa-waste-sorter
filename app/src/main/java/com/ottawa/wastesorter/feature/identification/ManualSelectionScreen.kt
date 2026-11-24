package com.ottawa.wastesorter.feature.identification

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.rules.ClassificationEngine
import com.ottawa.wastesorter.rules.Condition
import com.ottawa.wastesorter.rules.ItemComponent
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.rules.ObjectType
import com.ottawa.wastesorter.rules.OttawaRules2026
import com.ottawa.wastesorter.ui.theme.MANUAL_MATERIALS
import com.ottawa.wastesorter.ui.theme.label

private class ComponentDraft(material: Material? = null, condition: Condition = Condition.CLEAN_EMPTY) {
    var material by mutableStateOf(material)
    var condition by mutableStateOf(condition)
}

/**
 * Tri manuel, sans IA et sans connexion. Les sélections passent par le même moteur de règles.
 * La recherche filtre les matériaux par nom et par exemples d'objets (ex. « Pringles » → tube spiralé).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ManualSelectionScreen(
    language: String,
    onClassify: (itemName: String, objectType: ObjectType, components: List<ItemComponent>) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var itemName by rememberSaveable { mutableStateOf("") }
    var query by rememberSaveable { mutableStateOf("") }
    var objectType by rememberSaveable { mutableStateOf(ObjectType.FOOD_PACKAGING) }
    val components = remember { mutableStateListOf(ComponentDraft()) }

    // Index de recherche : libellé du matériau + exemples des règles officielles qui le concernent.
    val searchIndex = remember(language) {
        MANUAL_MATERIALS.associateWith { material ->
            val examples = OttawaRules2026.rules.filter { material in it.materials }.joinToString(" ") { it.examples.get(language) }
            ClassificationEngine.normalize(context.getString(material.label()) + " " + examples)
        }
    }
    val normalizedQuery = ClassificationEngine.normalize(query)
    val visibleMaterials = if (normalizedQuery.isEmpty()) MANUAL_MATERIALS
    else MANUAL_MATERIALS.filter { searchIndex[it]?.contains(normalizedQuery) == true }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.manual_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
        bottomBar = {
            val ready = components.all { it.material != null }
            Button(
                onClick = {
                    val name = itemName.ifBlank { context.getString(components.first().material!!.label()) }
                    onClassify(
                        name,
                        objectType,
                        components.mapIndexed { i, d ->
                            ItemComponent(
                                name = if (components.size == 1) name else "${context.getString(d.material!!.label())} (${i + 1})",
                                material = d.material!!,
                                condition = d.condition,
                            )
                        },
                    )
                },
                enabled = ready,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(52.dp),
            ) { Text(stringResource(R.string.manual_classify)) }
        },
    ) { padding ->
        LazyColumn(
            Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = itemName,
                    onValueChange = { itemName = it.take(80) },
                    label = { Text(stringResource(R.string.manual_item_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                Text(stringResource(R.string.manual_object_type), style = MaterialTheme.typography.titleSmall)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ObjectType.entries.forEach { type ->
                        FilterChip(
                            selected = objectType == type,
                            onClick = { objectType = type },
                            label = { Text(stringResource(type.label())) },
                        )
                    }
                }
            }
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    placeholder = { Text(stringResource(R.string.manual_search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            itemsIndexed(components) { index, draft ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                stringResource(R.string.manual_part, index + 1),
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.weight(1f),
                            )
                            if (components.size > 1) {
                                IconButton(onClick = { components.removeAt(index) }) {
                                    Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.action_delete))
                                }
                            }
                        }
                        Text(stringResource(R.string.manual_material), style = MaterialTheme.typography.labelLarge)
                        if (visibleMaterials.isEmpty()) {
                            Text(stringResource(R.string.manual_no_match), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Le matériau déjà choisi reste visible même s'il ne correspond plus à la recherche.
                            val shown = (visibleMaterials + listOfNotNull(draft.material)).distinct()
                            shown.forEach { material ->
                                FilterChip(
                                    selected = draft.material == material,
                                    onClick = { draft.material = material },
                                    label = { Text(stringResource(material.label())) },
                                )
                            }
                        }
                        Text(stringResource(R.string.manual_condition), style = MaterialTheme.typography.labelLarge)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Condition.entries.forEach { condition ->
                                FilterChip(
                                    selected = draft.condition == condition,
                                    onClick = { draft.condition = condition },
                                    label = { Text(stringResource(condition.label())) },
                                )
                            }
                        }
                    }
                }
            }
            if (components.size < 4) {
                item {
                    TextButton(onClick = { components.add(ComponentDraft()) }) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Text(stringResource(R.string.manual_add_part))
                    }
                }
            }
        }
    }
}
