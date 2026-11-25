package com.ottawa.wastesorter.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.PrivacyTip
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.BuildConfig
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.domain.model.AppSettings
import com.ottawa.wastesorter.domain.model.IdentifyException
import com.ottawa.wastesorter.feature.identification.messageRes
import com.ottawa.wastesorter.rules.OttawaRules2026
import com.ottawa.wastesorter.ui.components.CapsLabel
import com.ottawa.wastesorter.ui.components.Divider1
import com.ottawa.wastesorter.ui.components.OwsCard
import com.ottawa.wastesorter.ui.components.PrimaryButton
import com.ottawa.wastesorter.ui.components.ScreenTitle
import com.ottawa.wastesorter.ui.components.SecondaryButton
import com.ottawa.wastesorter.ui.components.TextAction
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class AppLanguage(val tag: String, val label: Int) {
    SYSTEM("", R.string.settings_lang_system),
    ENGLISH("en", R.string.settings_lang_en),
    FRENCH("fr", R.string.settings_lang_fr),
}

private enum class SettingsDialog { LANGUAGE, SERVER, PRIVACY, CLEAR, ABOUT }

/** Écran 11 — Réglages, et état S10 — dialogue « Effacer l'historique ». */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val settings by container.settings.settings.collectAsStateWithLifecycle(initialValue = AppSettings(BuildConfig.DEFAULT_BACKEND_URL, true))
    val countFlow = remember(container) { container.history.observe("").map { it.size } }
    val historyCount by countFlow.collectAsStateWithLifecycle(initialValue = 0)
    var dialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }

    val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags().let { tags ->
        when {
            tags.startsWith("fr") -> AppLanguage.FRENCH
            tags.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    Column(
        modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ScreenTitle(stringResource(R.string.nav_settings), Modifier.padding(top = 8.dp))

        Group(stringResource(R.string.settings_group_prefs)) {
            SettingRow(Icons.Rounded.Translate, stringResource(R.string.settings_language), stringResource(currentLanguage.label)) { dialog = SettingsDialog.LANGUAGE }
            Divider1()
            SettingRow(Icons.Rounded.LocationOn, stringResource(R.string.settings_municipality), stringResource(R.string.settings_municipality_value), trailing = false)
        }

        Group(stringResource(R.string.settings_group_identification)) {
            SettingRow(
                Icons.Rounded.Dns,
                stringResource(R.string.settings_server),
                settings.backendUrl.ifBlank { stringResource(R.string.settings_server_none) },
            ) { dialog = SettingsDialog.SERVER }
        }

        Group(stringResource(R.string.settings_group_data)) {
            SettingRow(Icons.Rounded.PrivacyTip, stringResource(R.string.settings_privacy_title), stringResource(R.string.settings_privacy_sub)) { dialog = SettingsDialog.PRIVACY }
            Divider1()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Icon(Icons.Rounded.Image, null, tint = Ows.Secondary)
                Column(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.settings_thumbnails), style = OwsType.Body.copy(fontWeight = FontWeight.SemiBold), color = Ows.Charcoal)
                    Text(stringResource(R.string.settings_thumbnails_help), style = OwsType.Meta, color = Ows.Secondary)
                }
                Switch(
                    checked = settings.saveThumbnails,
                    onCheckedChange = { enabled -> scope.launch { container.settings.setSaveThumbnails(enabled) } },
                    colors = SwitchDefaults.colors(checkedTrackColor = Ows.Green),
                )
            }
            Divider1()
            SettingRow(
                Icons.Rounded.DeleteSweep, stringResource(R.string.history_clear),
                stringResource(R.string.settings_saved_count, historyCount), color = Ows.Danger, trailing = false,
            ) { if (historyCount > 0) dialog = SettingsDialog.CLEAR }
        }

        Group(stringResource(R.string.settings_about)) {
            SettingRow(Icons.Rounded.Info, stringResource(R.string.settings_about_app), stringResource(R.string.settings_about_sub)) { dialog = SettingsDialog.ABOUT }
            Divider1()
            SettingRow(Icons.Rounded.Android, stringResource(R.string.settings_version), "${BuildConfig.VERSION_NAME} · ${OttawaRules2026.VERSION}", trailing = false)
        }
    }

    when (dialog) {
        SettingsDialog.LANGUAGE -> OwsDialog(onDismiss = { dialog = null }) {
            DialogTitle(stringResource(R.string.settings_language))
            AppLanguage.entries.forEach { lang ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp)).selectable(
                        selected = currentLanguage == lang,
                        role = Role.RadioButton,
                        onClick = {
                            dialog = null
                            AppCompatDelegate.setApplicationLocales(
                                if (lang == AppLanguage.SYSTEM) LocaleListCompat.getEmptyLocaleList() else LocaleListCompat.forLanguageTags(lang.tag),
                            )
                        },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = currentLanguage == lang, onClick = null, colors = RadioButtonDefaults.colors(selectedColor = Ows.Green))
                    Text(stringResource(lang.label), style = OwsType.Body, color = Ows.Charcoal, modifier = Modifier.padding(start = 12.dp))
                }
            }
        }
        SettingsDialog.SERVER -> ServerDialog(initial = settings.backendUrl, onDismiss = { dialog = null })
        SettingsDialog.PRIVACY -> OwsDialog(onDismiss = { dialog = null }) {
            DialogTitle(stringResource(R.string.settings_privacy_title))
            Text(stringResource(R.string.settings_privacy), style = OwsType.BodySmall, color = Ows.Secondary)
            PrimaryButton(stringResource(R.string.conf_ok), { dialog = null }, Modifier.fillMaxWidth())
        }
        SettingsDialog.CLEAR -> OwsDialog(onDismiss = { dialog = null }) {
            Icon(Icons.Rounded.DeleteSweep, null, tint = Ows.Danger, modifier = Modifier.size(28.dp).align(Alignment.CenterHorizontally))
            Text(
                stringResource(R.string.clear_title), style = OwsType.CardTitle, color = Ows.Charcoal,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Text(
                stringResource(R.string.clear_text, historyCount), style = OwsType.BodySmall, color = Ows.Secondary,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                TextAction(stringResource(R.string.action_cancel), { dialog = null })
                Box(
                    Modifier.height(44.dp).clip(RoundedCornerShape(22.dp)).background(Ows.Danger)
                        .clickable(role = Role.Button) {
                            dialog = null
                            scope.launch { withContext(Dispatchers.IO) { container.history.clear() } }
                        }.padding(horizontal = 20.dp),
                    contentAlignment = Alignment.Center,
                ) { Text(stringResource(R.string.clear_confirm), style = OwsType.Button.copy(fontSize = 15.sp), color = Color.White) }
            }
        }
        SettingsDialog.ABOUT -> AboutDialog(onDismiss = { dialog = null })
        null -> Unit
    }
}

@Composable
private fun Group(label: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CapsLabel(label, Modifier.padding(start = 4.dp))
        OwsCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    title: String,
    sub: String,
    color: Color = Ows.Charcoal,
    trailing: Boolean = true,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 64.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Icon(icon, null, tint = if (color == Ows.Charcoal) Ows.Secondary else color)
        Column(Modifier.weight(1f).padding(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = OwsType.Body.copy(fontWeight = FontWeight.SemiBold), color = color)
            Text(sub, style = OwsType.Meta, color = Ows.Secondary, maxLines = 2)
        }
        if (trailing) Icon(Icons.Rounded.ChevronRight, null, tint = Ows.Muted)
    }
}

@Composable
private fun OwsDialog(onDismiss: () -> Unit, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            Modifier.widthIn(max = 360.dp).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Ows.Surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            content = content,
        )
    }
}

@Composable
private fun DialogTitle(text: String) = Text(text, style = OwsType.CardTitle, color = Ows.Charcoal)

/** Adresse du serveur d'identification (Cloud Run) + test de connexion. */
@Composable
private fun ServerDialog(initial: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    var url by rememberSaveable { mutableStateOf(initial) }
    var status by remember { mutableStateOf<String?>(null) }
    var testing by remember { mutableStateOf(false) }

    OwsDialog(onDismiss) {
        DialogTitle(stringResource(R.string.settings_server))
        Text(stringResource(R.string.settings_server_help), style = OwsType.Meta.copy(fontSize = 14.sp, lineHeight = 20.sp), color = Ows.Secondary)
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(16.dp)).background(Ows.Surface)
                .border(2.dp, Ows.Green, RoundedCornerShape(16.dp)).padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            if (url.isEmpty()) Text("https://…run.app", style = OwsType.BodySmall, color = Ows.Muted)
            BasicTextField(
                value = url, onValueChange = { url = it.trim(); status = null }, singleLine = true,
                textStyle = OwsType.BodySmall.copy(color = Ows.Charcoal), cursorBrush = SolidColor(Ows.Green),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        status?.let { Text(it, style = OwsType.Meta, color = Ows.Charcoal) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SecondaryButton(
                stringResource(if (testing) R.string.settings_testing else R.string.settings_test),
                {
                    testing = true
                    scope.launch {
                        status = try {
                            val health = container.identification.checkHealth(url)
                            context.getString(if (health.aiConfigured) R.string.settings_test_ok else R.string.settings_test_no_ai, health.rulesVersion)
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: IdentifyException) {
                            context.getString(e.error.messageRes())
                        } catch (e: Exception) {
                            context.getString(R.string.error_network)
                        }
                        testing = false
                    }
                },
                Modifier.weight(1f),
                enabled = !testing && url.isNotBlank(),
            )
            PrimaryButton(stringResource(R.string.action_save), {
                scope.launch { container.settings.setBackendUrl(url) }
                onDismiss()
            }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun AboutDialog(onDismiss: () -> Unit) {
    val uri = LocalUriHandler.current
    OwsDialog(onDismiss) {
        DialogTitle(stringResource(R.string.settings_about_app))
        Text(
            stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME, OttawaRules2026.VERSION),
            style = OwsType.BodySmall, color = Ows.Secondary,
        )
        Column {
            TextAction(stringResource(R.string.settings_src_city), { uri.openUri(OttawaRules2026.SRC_CITY) })
            TextAction(stringResource(R.string.settings_src_circular), { uri.openUri(OttawaRules2026.SRC_CIRCULAR) })
            TextAction(stringResource(R.string.settings_src_guide), { uri.openUri(OttawaRules2026.SRC_CIRCULAR_GUIDE) })
        }
        Text(stringResource(R.string.settings_font_credit), style = OwsType.Meta.copy(fontSize = 12.sp), color = Ows.Muted)
        PrimaryButton(stringResource(R.string.conf_ok), onDismiss, Modifier.fillMaxWidth())
    }
}
