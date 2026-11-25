package com.ottawa.wastesorter.feature.camera

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import java.io.File

/** Écran 02 — Caméra (CameraX). La photo est écrite dans le cache, puis transmise à l'aperçu. */
@Composable
fun CameraScreen(onPhotoCaptured: (Uri) -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    LightStatusIcons()
    var hasPermission by remember { mutableStateOf(context.hasCameraPermission()) }
    var askedOnce by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
        askedOnce = true
    }
    LaunchedEffect(Unit) { if (!hasPermission) launcher.launch(Manifest.permission.CAMERA) }
    // Retour des réglages Android : on revérifie l'autorisation.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { hasPermission = context.hasCameraPermission() }

    Box(
        Modifier.fillMaxSize().background(
            Brush.radialGradient(listOf(Color(0xFF4A4640), Color(0xFF2A2926), Color(0xFF141515)), radius = 1400f),
        ),
    ) {
        if (hasPermission) {
            CameraContent(onPhotoCaptured, onGallery, onBack)
        } else {
            Column(
                Modifier.align(Alignment.Center).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(stringResource(R.string.camera_permission), color = Color.White, style = OwsType.Body, textAlign = TextAlign.Center)
                if (askedOnce) {
                    PrimaryButton(stringResource(R.string.camera_grant), { launcher.launch(Manifest.permission.CAMERA) }, Modifier.fillMaxWidth())
                    PrimaryButton(
                        stringResource(R.string.camera_open_settings),
                        {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        },
                        Modifier.fillMaxWidth(),
                        container = Color.White.copy(alpha = 0.14f),
                    )
                }
            }
            Box(Modifier.statusBarsPadding().padding(8.dp)) { RoundDarkButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = Color.White) } }
        }
    }
}

@Composable
private fun CameraContent(onPhotoCaptured: (Uri) -> Unit, onGallery: () -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER } }
    var flashOn by remember { mutableStateOf(false) }
    var useFront by remember { mutableStateOf(false) }
    val imageCapture = remember { ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build() }
    var capturing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf(false) }

    imageCapture.flashMode = if (flashOn) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF

    DisposableEffect(lifecycleOwner, useFront) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var provider: ProcessCameraProvider? = null
        providerFuture.addListener({
            runCatching {
                provider = providerFuture.get()
                val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val selector = if (useFront) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                provider?.unbindAll()
                provider?.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                error = false
            }.onFailure { error = true }
        }, ContextCompat.getMainExecutor(context))
        onDispose { provider?.unbindAll() }
    }

    Box(Modifier.fillMaxSize()) {
        AndroidView(factory = { previewView }, modifier = Modifier.fillMaxSize())

        Column(Modifier.fillMaxSize()) {
            // Barre du haut : retour + flash
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RoundDarkButton(onBack) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), tint = Color.White) }
                Spacer(Modifier.weight(1f))
                Row(
                    Modifier.height(40.dp).clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.35f))
                        .clickable(role = Role.Switch) { flashOn = !flashOn }.padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(if (flashOn) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff, stringResource(R.string.camera_flash), tint = Color.White, modifier = Modifier.size(20.dp))
                    Text(stringResource(if (flashOn) R.string.camera_flash_on else R.string.camera_flash_off), color = Color.White, style = OwsType.Meta.copy(fontWeight = FontWeight.SemiBold))
                }
            }

            // Cadre de visée + consigne
            Column(
                Modifier.weight(1f).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Box(Modifier.size(width = 260.dp, height = 300.dp).drawBehind { viewfinderCorners() })
                Spacer(Modifier.height(20.dp))
                Text(
                    stringResource(if (error) R.string.camera_error else R.string.camera_hint),
                    color = Color.White,
                    style = OwsType.Meta.copy(fontWeight = FontWeight.SemiBold, fontSize = OwsType.BodySmall.fontSize),
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 16.dp, vertical = 10.dp),
                )
            }

            // Barre du bas : galerie, déclencheur, changement de caméra
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier.size(52.dp).clip(RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF7FA98F), Color(0xFF3E6A55))))
                        .border(2.dp, Color.White.copy(alpha = 0.8f), RoundedCornerShape(14.dp))
                        .clickable(role = Role.Button, onClick = onGallery),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.PhotoLibrary, stringResource(R.string.home_upload_photo), tint = Color.White, modifier = Modifier.size(22.dp)) }

                Box(
                    Modifier.size(80.dp).clip(CircleShape).border(4.dp, Color.White, CircleShape)
                        .clickable(enabled = !capturing, role = Role.Button) {
                            capturing = true
                            takePhoto(context, imageCapture,
                                onSaved = { uri -> capturing = false; onPhotoCaptured(uri) },
                                onError = { capturing = false; error = true },
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (capturing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(40.dp))
                    else Box(Modifier.size(62.dp).clip(CircleShape).background(Color.White))
                }

                Box(
                    Modifier.size(52.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                        .clickable(role = Role.Button) { useFront = !useFront },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Cameraswitch, stringResource(R.string.camera_switch), tint = Color.White) }
            }
        }
    }
}

@Composable
private fun RoundDarkButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.35f)).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

/** Quatre coins arrondis blancs (3 dp) du cadre de visée. */
private fun DrawScope.viewfinderCorners() {
    val stroke = 3.dp.toPx()
    val len = 36.dp.toPx()
    val r = 16.dp.toPx()
    val w = size.width
    val h = size.height
    val style = Stroke(width = stroke, cap = StrokeCap.Round)
    // Chaque coin : un arc de cercle + deux segments.
    listOf(
        Triple(Offset(0f, 0f), 180f, Pair(Offset(r, 0f) to Offset(len, 0f), Offset(0f, r) to Offset(0f, len))),
        Triple(Offset(w - 2 * r, 0f), 270f, Pair(Offset(w - r, 0f) to Offset(w - len, 0f), Offset(w, r) to Offset(w, len))),
        Triple(Offset(0f, h - 2 * r), 90f, Pair(Offset(0f, h - r) to Offset(0f, h - len), Offset(r, h) to Offset(len, h))),
        Triple(Offset(w - 2 * r, h - 2 * r), 0f, Pair(Offset(w, h - r) to Offset(w, h - len), Offset(w - r, h) to Offset(w - len, h))),
    ).forEach { (arcTopLeft, startAngle, lines) ->
        drawArc(Color.White, startAngle, 90f, false, topLeft = arcTopLeft, size = Size(2 * r, 2 * r), style = style)
        drawLine(Color.White, lines.first.first, lines.first.second, stroke, StrokeCap.Round)
        drawLine(Color.White, lines.second.first, lines.second.second, stroke, StrokeCap.Round)
    }
}

/** Icônes de la barre d'état en clair sur fond sombre, le temps de l'écran caméra. */
@Composable
private fun LightStatusIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previous = controller?.isAppearanceLightStatusBars
        controller?.isAppearanceLightStatusBars = false
        onDispose { if (previous != null) controller?.isAppearanceLightStatusBars = previous }
    }
}

private fun Context.hasCameraPermission() =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private fun takePhoto(context: Context, imageCapture: ImageCapture, onSaved: (Uri) -> Unit, onError: () -> Unit) {
    val dir = File(context.cacheDir, "captures").apply { mkdirs() }
    val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
    val options = ImageCapture.OutputFileOptions.Builder(file).build()
    imageCapture.takePicture(
        options,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) = onSaved(Uri.fromFile(file))
            override fun onError(exception: ImageCaptureException) {
                file.delete()
                onError()
            }
        },
    )
}
