package com.ottawa.wastesorter.feature.identification

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.domain.model.IdentifyError
import com.ottawa.wastesorter.ui.components.CloseButton
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.components.ScreenTitle
import com.ottawa.wastesorter.ui.components.SecondaryButton
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType

/** Écran 03 — Aperçu : l'utilisateur confirme la photo avant tout appel à l'IA. */
@Composable
fun PhotoPreviewScreen(vm: IdentificationViewModel, onRetake: () -> Unit, onContinue: () -> Unit, onClose: () -> Unit) {
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val uri = state.photoUri
    val bitmap by produceState<ImageBitmap?>(initialValue = null, uri) {
        value = uri?.let { context.appContainer.imageProcessor.loadPreview(it)?.asImageBitmap() }
    }

    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(start = 16.dp, end = 16.dp, bottom = 24.dp)) {
        Box(Modifier.heightIn(min = 64.dp).padding(start = 0.dp), contentAlignment = Alignment.CenterStart) {
            CloseButton(onClose)
        }
        Column(Modifier.padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            ScreenTitle(stringResource(R.string.preview_title))
            Text(stringResource(R.string.preview_subtitle), style = OwsType.Body, color = Ows.Secondary)
        }
        Box(
            Modifier.weight(1f).heightIn(min = 300.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Ows.Divider),
            contentAlignment = Alignment.Center,
        ) {
            val bmp = bitmap
            if (bmp != null) {
                Image(bmp, stringResource(R.string.preview_title), contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                CircularProgressIndicator(color = Ows.Green)
            }
        }
        Row(Modifier.padding(top = 24.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SecondaryButton(stringResource(R.string.preview_retake), onRetake, Modifier.weight(1f), icon = Icons.Rounded.Refresh)
            PrimaryButton(
                stringResource(R.string.preview_continue), onContinue, Modifier.weight(1f),
                trailingIcon = Icons.AutoMirrored.Rounded.ArrowForward, enabled = uri != null,
            )
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

@StringRes
fun IdentifyError.titleRes(): Int = when (this) {
    IdentifyError.NotConfigured -> R.string.error_not_configured_title
    IdentifyError.InvalidImage -> R.string.error_invalid_image_title
    IdentifyError.RateLimited -> R.string.error_rate_limited_title
    else -> R.string.error_network_title
}
