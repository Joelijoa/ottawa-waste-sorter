package com.ottawa.wastesorter.feature.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.domain.model.HistoryItem
import com.ottawa.wastesorter.feature.history.HistoryRow

@Composable
fun HomeScreen(
    onTakePhoto: () -> Unit,
    onUploadPhoto: () -> Unit,
    onManual: () -> Unit,
    onGuide: () -> Unit,
    onOpenHistoryItem: (HistoryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val recentFlow = remember(context) { context.appContainer.history.observeRecent(5) }
    val recent by recentFlow.collectAsStateWithLifecycle(initialValue = emptyList())

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(24.dp))
                Image(
                    painter = painterResource(R.drawable.ic_launcher_foreground),
                    contentDescription = null,
                    modifier = Modifier.size(96.dp),
                )
                Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
                Text(
                    stringResource(R.string.home_tagline),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                )
            }
        }
        item {
            Button(onClick = onTakePhoto, modifier = Modifier.fillMaxWidth().height(56.dp)) {
                Icon(Icons.Filled.CameraAlt, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_take_photo))
            }
        }
        item {
            FilledTonalButton(onClick = onUploadPhoto, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                Icon(Icons.Filled.PhotoLibrary, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.home_upload_photo))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = onManual, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Filled.Search, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.home_manual))
                }
                OutlinedButton(onClick = onGuide, modifier = Modifier.weight(1f)) {
                    Icon(Icons.AutoMirrored.Filled.MenuBook, contentDescription = null)
                    Spacer(Modifier.size(6.dp))
                    Text(stringResource(R.string.home_guide))
                }
            }
        }
        if (recent.isNotEmpty()) {
            item {
                Text(
                    stringResource(R.string.home_recent),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            items(recent, key = { it.id }) { item ->
                HistoryRow(item = item, onClick = { onOpenHistoryItem(item) }, onDelete = null)
            }
        }
    }
}
