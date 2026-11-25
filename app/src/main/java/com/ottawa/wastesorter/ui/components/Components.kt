package com.ottawa.wastesorter.ui.components

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.rules.Material
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import com.ottawa.wastesorter.ui.theme.look
import com.ottawa.wastesorter.ui.theme.style
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

// ───────────────────────── Surfaces ─────────────────────────

/** Carte blanche à contour 1 dp (« surface » du design). */
@Composable
fun OwsCard(
    modifier: Modifier = Modifier,
    radius: Dp = 20.dp,
    onClick: (() -> Unit)? = null,
    padding: PaddingValues = PaddingValues(0.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(radius)
    Column(
        modifier
            .clip(shape)
            .background(Ows.Surface)
            .border(1.dp, Ows.Border, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

@Composable
fun Divider1() = Box(Modifier.fillMaxWidth().height(1.dp).background(Ows.Divider))

// ───────────────────────── Boutons ─────────────────────────

/** Bouton principal : 52 dp de haut, rayon 16 dp, pleine largeur. */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    enabled: Boolean = true,
    container: Color = Ows.Green,
    content: Color = Color.White,
) = OwsButtonBase(text, onClick, modifier, icon, trailingIcon, enabled, container, content, null)

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
) = OwsButtonBase(text, onClick, modifier, icon, null, enabled, Color.Transparent, Ows.Charcoal, Ows.Outline)

@Composable
fun TonalButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) =
    OwsButtonBase(text, onClick, modifier, icon, null, true, Ows.GreenSoft, Ows.GreenDark, null)

@Composable
private fun OwsButtonBase(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    icon: ImageVector?,
    trailingIcon: ImageVector?,
    enabled: Boolean,
    container: Color,
    content: Color,
    border: Color?,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .heightIn(min = 52.dp)
            .clip(shape)
            .background(if (enabled) container else container.copy(alpha = 0.5f))
            .then(if (border != null) Modifier.border(1.5.dp, border, shape) else Modifier)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = content, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = OwsType.Button, color = content, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) {
            Spacer(Modifier.width(8.dp))
            Icon(trailingIcon, null, tint = content, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, color: Color = Ows.Green) {
    Box(
        modifier.heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp)).clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = OwsType.Button.copy(fontSize = 15.sp), color = color) }
}

/** Bouton d'en-tête rond de 48 dp (retour, fermer, partager). */
@Composable
fun TopIconButton(icon: ImageVector, description: String, onClick: () -> Unit, tint: Color = Ows.Charcoal) {
    IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
        Icon(icon, contentDescription = description, tint = tint)
    }
}

@Composable
fun BackButton(onClick: () -> Unit, tint: Color = Ows.Charcoal) =
    TopIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.action_back), onClick, tint)

@Composable
fun CloseButton(onClick: () -> Unit) =
    TopIconButton(Icons.Rounded.Close, stringResource(R.string.action_close), onClick)

// ───────────────────────── Titres ─────────────────────────

@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = OwsType.ScreenTitle, color = Ows.Charcoal, modifier = modifier.semantics { heading() })

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier) =
    Text(text, style = OwsType.Section, color = Ows.Charcoal, modifier = modifier.semantics { heading() })

@Composable
fun CapsLabel(text: String, modifier: Modifier = Modifier, color: Color = Ows.Muted) =
    Text(text.uppercase(), style = OwsType.Label, color = color, modifier = modifier)

// ───────────────────────── Notes ─────────────────────────

/** Note d'information (fond vert clair) : explique une règle. */
@Composable
fun InfoNote(text: String, modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Info) =
    Note(text, null, icon, Ows.GreenSoft, Ows.GreenDark, Ows.GreenInk, modifier)

/** Note d'avertissement (fond ambre) : demande de l'attention. */
@Composable
fun WarningNote(title: String?, text: String, icon: ImageVector, modifier: Modifier = Modifier) =
    Note(text, title, icon, Ows.WarningSoft, Ows.WarningIcon, Ows.WarningInk2, modifier)

@Composable
private fun Note(text: String, title: String?, icon: ImageVector, bg: Color, iconColor: Color, ink: Color, modifier: Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = iconColor, modifier = Modifier.size(22.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (title != null) Text(title, style = OwsType.BodySmall.copy(fontWeight = FontWeight.Bold), color = Ows.WarningInk)
            Text(text, style = OwsType.Meta.copy(fontSize = 14.sp, lineHeight = 21.sp), color = ink)
        }
    }
}

// ───────────────────────── Recherche ─────────────────────────

/** Barre de recherche au repos (pilule 56 dp). Ouvre l'écran de recherche. */
@Composable
fun SearchPill(placeholder: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector) {
    Row(
        modifier.fillMaxWidth().height(56.dp).clip(RoundedCornerShape(28.dp)).background(Ows.Surface)
            .border(1.dp, Ows.Border, RoundedCornerShape(28.dp)).clickable(onClick = onClick).padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(icon, null, tint = Ows.Secondary)
        Text(placeholder, style = OwsType.Body, color = Ows.Muted)
    }
}

// ───────────────────────── Bacs ─────────────────────────

/** Petit carré de couleur du bac (toujours accompagné de son nom). */
@Composable
fun BinSwatch(destination: Destination, size: Dp = 14.dp) {
    val style = destination.style()
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size / 3.5f)).background(style.bg)
            .border(1.dp, Ows.Charcoal.copy(alpha = 0.15f), RoundedCornerShape(size / 3.5f)),
    )
}

@Composable
fun BinLine(destinations: List<Destination>, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle = OwsType.Meta) {
    val names = destinations.map { stringResource(it.style().name) }
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        destinations.take(3).forEach { BinSwatch(it) }
        Text(names.joinToString(" + "), style = style, color = Ows.Secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Carré d'icône du bac (catégories, guide). */
@Composable
fun BinIconTile(destination: Destination, size: Dp, iconSize: Dp, radius: Dp, onBin: Boolean = false) {
    val s = destination.style()
    Box(
        Modifier.size(size).clip(RoundedCornerShape(radius)).background(if (onBin) s.chip else s.bg)
            .then(if (onBin) Modifier else Modifier.border(1.dp, Ows.Charcoal.copy(alpha = 0.1f), RoundedCornerShape(radius))),
        contentAlignment = Alignment.Center,
    ) { Icon(s.icon, null, tint = s.fg, modifier = Modifier.size(iconSize)) }
}

// ───────────────────────── Vignettes ─────────────────────────

/**
 * Vignette d'un objet : photo (fichier local ou URI) si disponible, sinon icône du matériau.
 */
@Composable
fun ItemThumb(
    material: Material,
    size: Dp,
    radius: Dp,
    iconSize: Dp,
    photoPath: String? = null,
    photoUri: Uri? = null,
    shape: Shape = RoundedCornerShape(radius),
) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(null, photoPath, photoUri) {
        value = withContext(Dispatchers.IO) {
            when {
                photoPath != null && File(photoPath).exists() -> BitmapFactory.decodeFile(photoPath)?.asImageBitmap()
                photoUri != null -> context.appContainer.imageProcessor.loadPreview(photoUri)?.asImageBitmap()
                else -> null
            }
        }
    }
    val look = material.look()
    Box(Modifier.size(size).clip(shape).background(look.bg), contentAlignment = Alignment.Center) {
        val bmp = bitmap
        if (bmp != null) {
            Image(bmp, null, contentScale = ContentScale.Crop, modifier = Modifier.size(size))
        } else {
            Icon(look.icon, null, tint = look.fg, modifier = Modifier.size(iconSize))
        }
    }
}

// ───────────────────────── Listes ─────────────────────────

/** Ligne de liste : vignette + titre + bac + chevron (accueil, historique, recherche). */
@Composable
fun ItemRow(
    title: String,
    material: Material,
    destinations: List<Destination>,
    onClick: () -> Unit,
    photoPath: String? = null,
    trailingText: String? = null,
    thumbSize: Dp = 52.dp,
    showChevron: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        ItemThumb(material, thumbSize, 14.dp, thumbSize / 2, photoPath = photoPath)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title, style = OwsType.Body.copy(fontWeight = FontWeight.SemiBold), color = Ows.Charcoal,
                    maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f),
                )
                if (trailingText != null) Text(trailingText, style = OwsType.Meta.copy(fontSize = 12.sp), color = Ows.Muted)
            }
            BinLine(destinations)
        }
        if (showChevron) Icon(Icons.Rounded.ChevronRight, null, tint = Ows.Muted)
    }
}

// ───────────────────────── Confiance ─────────────────────────

/** Barres de confiance : jamais aux couleurs des bacs (ce n'est pas un verdict officiel). */
@Composable
fun ConfidenceBars(level: Int, barWidth: Dp = 6.dp, barHeight: Dp = 14.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        (1..3).forEach { n ->
            Box(Modifier.width(barWidth).height(barHeight).clip(RoundedCornerShape(3.dp)).background(if (n <= level) Ows.Green else Ows.BarOff))
        }
    }
}

/** 3 = forte, 2 = probable, 1 = possible. */
fun confidenceLevel(confidence: Double?): Int = when {
    confidence == null -> 3
    confidence >= 0.85 -> 3
    confidence >= 0.6 -> 2
    else -> 1
}

fun confidenceLabelRes(level: Int): Int = when (level) {
    3 -> R.string.conf_high
    2 -> R.string.conf_likely
    else -> R.string.conf_possible
}

// ───────────────────────── Sélection ─────────────────────────

/** Tuile de sélection (matériau) : fond vert clair + anneau 2 dp quand sélectionnée. */
@Composable
fun SelectTile(
    text: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingCheck: ImageVector? = null,
) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier.heightIn(min = 56.dp).clip(shape).background(if (selected) Ows.GreenSoft else Ows.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) Ows.Green else Ows.Border, shape)
            .clickable(role = Role.RadioButton, onClick = onClick).padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(icon, null, tint = if (selected) Ows.GreenDark else Ows.Secondary, modifier = Modifier.size(22.dp))
        Text(text, style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 18.sp), color = Ows.Charcoal, modifier = Modifier.weight(1f))
        if (selected && trailingCheck != null) Icon(trailingCheck, null, tint = Ows.Green, modifier = Modifier.size(20.dp))
    }
}

/** Puce « recherche récente » (36 dp, rayon 10). */
@Composable
fun HistoryChip(text: String, icon: ImageVector, onClick: () -> Unit) {
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.height(36.dp).clip(shape).background(Ows.Surface).border(1.dp, Ows.Border, shape)
            .clickable(onClick = onClick).padding(start = 10.dp, end = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, null, tint = Ows.Muted, modifier = Modifier.size(18.dp))
        Text(text, style = OwsType.Meta.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold), color = Ows.Charcoal)
    }
}

/** Petite étiquette (matériau, état). */
@Composable
fun Tag(text: String) {
    val shape = RoundedCornerShape(8.dp)
    Box(Modifier.height(26.dp).clip(shape).background(Ows.Surface).border(1.dp, Ows.Border, shape).padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
        Text(text, style = OwsType.Meta.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold), color = Ows.Charcoal)
    }
}

/** Pastille numérotée (étapes). */
@Composable
fun StepNumber(n: Int, bg: Color, fg: Color, size: Dp = 24.dp) {
    Box(Modifier.size(size).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) {
        Text("$n", style = OwsType.Meta.copy(fontWeight = FontWeight.Bold), color = fg)
    }
}

@Composable
fun RowScope.WeightSpacer() = Spacer(Modifier.weight(1f))

/** Exécute [block] une seule fois par valeur de [key] (événements ponctuels). */
@Composable
fun OnceEffect(key: Any?, block: suspend () -> Unit) {
    val done = remember(key) { mutableStateOf(false) }
    LaunchedEffect(key) {
        if (!done.value) {
            done.value = true
            block()
        }
    }
}
