package com.ottawa.wastesorter.feature.identification

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.domain.model.IdentifyError

/** Aperçu : l'utilisateur confirme la photo avant tout appel à l'IA (pas d'analyse automatique). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoPreviewScreen(
    vm: IdentificationViewModel,
    language: String,
    onRetake: () -> Unit,
    onManual: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uri = state.photoUri
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = uri?.let { context.appContainer.imageProcessor.loadPreview(it)?.asImageBitmap() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.preview_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.action_back))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(24.dp)),
                contentAlignment = Alignment.Center,
            ) {
                val bmp = bitmap
                if (bmp != null) {
                    Image(bmp, contentDescription = stringResource(R.string.preview_title), contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
                } else {
                    CircularProgressIndicator()
                }
            }

            state.error?.let { error ->
                ErrorCard(
                    error = error,
                    onRetry = { vm.analyze(language) },
                    onManual = onManual,
                    onOpenSettings = onOpenSettings,
                )
            }

            if (state.isLoading) {
                Row(
                    Modifier.fillMaxWidth().height(56.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator(Modifier.size(28.dp))
                    Spacer(Modifier.size(12.dp))
                    Text(stringResource(R.string.preview_analyzing))
                }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onRetake, modifier = Modifier.weight(1f).height(52.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.preview_retake))
                    }
                    Button(
                        onClick = { vm.analyze(language) },
                        enabled = uri != null,
                        modifier = Modifier.weight(1f).height(52.dp),
                    ) {
                        Text(stringResource(R.string.preview_identify))
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(error: IdentifyError, onRetry: () -> Unit, onManual: () -> Unit, onOpenSettings: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                stringResource(error.messageRes()),
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Start,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (error == IdentifyError.NotConfigured) {
                    TextButton(onClick = onOpenSettings) { Text(stringResource(R.string.nav_settings)) }
                } else if (error != IdentifyError.InvalidImage) {
                    TextButton(onClick = onRetry) { Text(stringResource(R.string.action_retry)) }
                }
                TextButton(onClick = onManual) { Text(stringResource(R.string.home_manual)) }
            }
        }
    }
}

@StringRes
fun IdentifyError.messageRes(): Int = when (this) {
    IdentifyError.NotConfigured -> R.string.error_not_configured
    IdentifyError.Network -> R.string.error_network
    IdentifyError.RateLimited -> R.string.error_rate_limited
    IdentifyError.InvalidImage -> R.string.error_invalid_image
    IdentifyError.AiUnavailable -> R.string.error_ai_unavailable
    is IdentifyError.Server -> R.string.error_server
}
