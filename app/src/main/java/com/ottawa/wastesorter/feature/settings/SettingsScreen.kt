package com.ottawa.wastesorter.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ottawa.wastesorter.BuildConfig
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.domain.model.AppSettings
import com.ottawa.wastesorter.domain.model.IdentifyException
import com.ottawa.wastesorter.feature.identification.messageRes
import com.ottawa.wastesorter.rules.OttawaRules2026
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private enum class AppLanguage(val tag: String, val label: Int) {
    SYSTEM("", R.string.settings_lang_system),
    ENGLISH("en", R.string.settings_lang_en),
    FRENCH("fr", R.string.settings_lang_fr),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val container = context.appContainer
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current
    val settings by container.settings.settings.collectAsStateWithLifecycle(
        initialValue = AppSettings(BuildConfig.DEFAULT_BACKEND_URL, true),
    )

    var urlDraft by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(settings.backendUrl) { if (urlDraft == null) urlDraft = settings.backendUrl }
    var testResult by remember { mutableStateOf<String?>(null) }

    val currentLanguage = AppCompatDelegate.getApplicationLocales().toLanguageTags().let { tags ->
        when {
            tags.startsWith("fr") -> AppLanguage.FRENCH
            tags.startsWith("en") -> AppLanguage.ENGLISH
            else -> AppLanguage.SYSTEM
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text(stringResource(R.string.nav_settings)) }) },
    ) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ── Langue ──
            Text(stringResource(R.string.settings_language), style = MaterialTheme.typography.titleMedium)
            AppLanguage.entries.forEach { lang ->
                Row(
                    Modifier.fillMaxWidth().selectable(
                        selected = currentLanguage == lang,
                        role = Role.RadioButton,
                        onClick = {
                            AppCompatDelegate.setApplicationLocales(
                                if (lang == AppLanguage.SYSTEM) LocaleListCompat.getEmptyLocaleList()
                                else LocaleListCompat.forLanguageTags(lang.tag),
                            )
                        },
                    ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = currentLanguage == lang, onClick = null)
                    Text(stringResource(lang.label), modifier = Modifier.padding(start = 8.dp))
                }
            }

            HorizontalDivider()

            // ── Serveur d'identification ──
            Text(stringResource(R.string.settings_server), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_server_help),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = urlDraft.orEmpty(),
                onValueChange = { urlDraft = it; testResult = null },
                label = { Text(stringResource(R.string.settings_server_url)) },
                placeholder = { Text("https://…run.app") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(onClick = {
                    scope.launch { container.settings.setBackendUrl(urlDraft.orEmpty()) }
                    testResult = context.getString(R.string.settings_saved)
                }) { Text(stringResource(R.string.action_save)) }
                OutlinedButton(onClick = {
                    scope.launch {
                        testResult = try {
                            val health = container.identification.checkHealth(urlDraft.orEmpty())
                            context.getString(
                                if (health.aiConfigured) R.string.settings_test_ok else R.string.settings_test_no_ai,
                                health.rulesVersion,
                            )
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: IdentifyException) {
                            context.getString(e.error.messageRes())
                        } catch (e: Exception) {
                            context.getString(R.string.error_network)
                        }
                    }
                }) { Text(stringResource(R.string.settings_test)) }
            }
            testResult?.let { Text(it, style = MaterialTheme.typography.bodySmall) }

            HorizontalDivider()

            // ── Confidentialité ──
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.settings_thumbnails), style = MaterialTheme.typography.titleMedium)
                    Text(
                        stringResource(R.string.settings_thumbnails_help),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.saveThumbnails,
                    onCheckedChange = { enabled -> scope.launch { container.settings.setSaveThumbnails(enabled) } },
                )
            }
            Text(
                stringResource(R.string.settings_privacy),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            HorizontalDivider()

            // ── À propos ──
            Text(stringResource(R.string.settings_about), style = MaterialTheme.typography.titleMedium)
            Text(
                stringResource(R.string.settings_about_text, BuildConfig.VERSION_NAME, OttawaRules2026.VERSION),
                style = MaterialTheme.typography.bodySmall,
            )
            TextButton(onClick = { uriHandler.openUri(OttawaRules2026.SRC_CITY) }) { Text(stringResource(R.string.settings_src_city)) }
            TextButton(onClick = { uriHandler.openUri(OttawaRules2026.SRC_CIRCULAR) }) { Text(stringResource(R.string.settings_src_circular)) }
        }
    }
}
