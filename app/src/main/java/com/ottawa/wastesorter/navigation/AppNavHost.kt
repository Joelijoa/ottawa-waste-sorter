package com.ottawa.wastesorter.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.feature.camera.CameraScreen
import com.ottawa.wastesorter.feature.guide.GuideDetailScreen
import com.ottawa.wastesorter.feature.guide.GuideScreen
import com.ottawa.wastesorter.feature.history.HistoryScreen
import com.ottawa.wastesorter.feature.home.HomeScreen
import com.ottawa.wastesorter.feature.identification.AnalysisScreen
import com.ottawa.wastesorter.feature.identification.DetailsScreen
import com.ottawa.wastesorter.feature.identification.IdentificationViewModel
import com.ottawa.wastesorter.feature.identification.LowConfidenceScreen
import com.ottawa.wastesorter.feature.identification.NavEvent
import com.ottawa.wastesorter.feature.identification.PhotoPreviewScreen
import com.ottawa.wastesorter.feature.identification.ResultScreen
import com.ottawa.wastesorter.feature.identification.UnknownScreen
import com.ottawa.wastesorter.feature.search.SearchScreen
import com.ottawa.wastesorter.feature.settings.SettingsScreen
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.OwsType

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val GUIDE = "guide"
    const val GUIDE_DETAIL = "guide/{bin}"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val PREVIEW = "preview"
    const val ANALYZE = "analyze"
    const val LOW_CONFIDENCE = "lowconf"
    const val UNKNOWN = "unknown"
    const val RESULT = "result"
    const val SEARCH = "search"
    const val DETAILS = "details"

    fun guideDetail(bin: Destination) = "guide/${bin.name}"
}

private data class Tab(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val TABS = listOf(
    Tab(Routes.HOME, R.string.nav_home, Icons.Rounded.Home),
    Tab(Routes.HISTORY, R.string.nav_history, Icons.Rounded.History),
    Tab(Routes.GUIDE, R.string.nav_guide, Icons.AutoMirrored.Rounded.MenuBook),
    Tab(Routes.SETTINGS, R.string.nav_settings, Icons.Rounded.Settings),
)

/** Onglet actif pour chaque écran qui affiche la barre de navigation (le détail de bac reste dans « Guide »). */
private fun tabOf(route: String?): String? = when (route) {
    Routes.HOME, Routes.HISTORY, Routes.GUIDE, Routes.SETTINGS -> route
    Routes.GUIDE_DETAIL -> Routes.GUIDE
    else -> null
}

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val context = LocalContext.current
    // Portée activité : partagé par tout le parcours photo, le tri manuel et le résultat.
    val idVm: IdentificationViewModel = viewModel(factory = IdentificationViewModel.factory(context.appContainer))
    val idState by idVm.state.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language

    val backStack by nav.currentBackStackEntryAsState()
    val currentTab = tabOf(backStack?.destination?.route)

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            idVm.setPhoto(uri)
            nav.navigate(Routes.PREVIEW) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
        }
    }
    val openGallery = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
    val goCamera = {
        idVm.discardPhoto()
        nav.navigate(Routes.CAMERA) { popUpTo(Routes.HOME) }
    }
    val goSearch = { nav.navigate(Routes.SEARCH) { launchSingleTop = true } }

    // Navigation pilotée par le ViewModel après une analyse ou un tri manuel.
    LaunchedEffect(idState.event) {
        val target = when (idState.event) {
            NavEvent.RESULT -> Routes.RESULT
            NavEvent.LOW_CONFIDENCE -> Routes.LOW_CONFIDENCE
            NavEvent.UNKNOWN -> Routes.UNKNOWN
            null -> null
        }
        if (target != null) {
            nav.navigate(target) {
                popUpTo(Routes.HOME)
                launchSingleTop = true
            }
            idVm.consumeEvent()
        }
    }

    Scaffold(
        containerColor = Ows.Background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = { if (currentTab != null) BottomBar(currentTab) { nav.navigateTab(it) } },
    ) { innerPadding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(innerPadding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onTakePhoto = goCamera,
                    onUploadPhoto = openGallery,
                    onSearch = goSearch,
                    onOpenBin = { nav.navigate(Routes.guideDetail(it)) },
                    onOpenHistoryItem = { idVm.showOutcome(it.outcome) },
                    onSeeAllHistory = { nav.navigateTab(Routes.HISTORY) },
                    modifier = Modifier.statusBarsPadding(),
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpen = { idVm.showOutcome(it.outcome) }, onTakePhoto = goCamera, modifier = Modifier.statusBarsPadding())
            }
            composable(Routes.GUIDE) {
                GuideScreen(language, onOpenBin = { nav.navigate(Routes.guideDetail(it)) }, modifier = Modifier.statusBarsPadding())
            }
            composable(Routes.GUIDE_DETAIL, arguments = listOf(navArgument("bin") { type = NavType.StringType })) { entry ->
                val bin = runCatching { Destination.valueOf(entry.arguments?.getString("bin").orEmpty()) }.getOrDefault(Destination.BLACK_BIN)
                GuideDetailScreen(bin, language, onBack = { nav.popBackStack() })
            }
            composable(Routes.SETTINGS) { SettingsScreen(Modifier.statusBarsPadding()) }

            composable(Routes.CAMERA) {
                CameraScreen(
                    onPhotoCaptured = { uri ->
                        idVm.setPhoto(uri)
                        nav.navigate(Routes.PREVIEW) { popUpTo(Routes.CAMERA) { inclusive = true } }
                    },
                    onGallery = openGallery,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.PREVIEW) {
                PhotoPreviewScreen(
                    vm = idVm,
                    onRetake = goCamera,
                    onContinue = {
                        idVm.analyze(language)
                        nav.navigate(Routes.ANALYZE)
                    },
                    onClose = { idVm.discardPhoto(); nav.popBackStack(Routes.HOME, inclusive = false) },
                )
            }
            composable(Routes.ANALYZE) {
                AnalysisScreen(
                    vm = idVm,
                    onCancel = { idVm.cancelAnalysis(); idVm.clearError(); nav.popBackStack() },
                    onSearch = { idVm.clearError(); goSearch() },
                    onSettings = { idVm.clearError(); nav.navigateTab(Routes.SETTINGS) },
                    onRetake = { idVm.clearError(); goCamera() },
                    onRetry = { idVm.clearError(); idVm.analyze(language) },
                )
            }
            composable(Routes.LOW_CONFIDENCE) {
                LowConfidenceScreen(
                    vm = idVm,
                    language = language,
                    onClose = { idVm.discardPhoto(); nav.popBackStack(Routes.HOME, inclusive = false) },
                    onManual = {
                        idVm.startDetails(idVm.state.value.outcome?.itemName.orEmpty(), idVm.state.value.outcome?.primaryMaterial ?: com.ottawa.wastesorter.rules.Material.PAPERBOARD)
                        nav.navigate(Routes.DETAILS)
                    },
                )
            }
            composable(Routes.UNKNOWN) {
                UnknownScreen(
                    onClose = { idVm.discardPhoto(); nav.popBackStack(Routes.HOME, inclusive = false) },
                    onRetake = goCamera,
                    onManual = {
                        idVm.startDetails(context.getString(R.string.details_unidentified))
                        nav.navigate(Routes.DETAILS)
                    },
                )
            }
            composable(Routes.RESULT) {
                ResultScreen(
                    vm = idVm,
                    language = language,
                    onBack = { idVm.discardPhoto(); if (!nav.popBackStack()) nav.navigateTab(Routes.HOME) },
                    onIdentifyAnother = goCamera,
                    onSearch = goSearch,
                    onViewHistory = { nav.navigateTab(Routes.HISTORY) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    language = language,
                    onBack = { nav.popBackStack() },
                    onOpenItem = { item ->
                        if (item.isMulti) {
                            idVm.classifyCatalog(item, language)
                        } else {
                            idVm.startDetails(item.name.get(language), catalog = item)
                            nav.navigate(Routes.DETAILS)
                        }
                    },
                    onMaterial = { name, material ->
                        idVm.startDetails(name, material)
                        nav.navigate(Routes.DETAILS)
                    },
                    onTakePhoto = goCamera,
                )
            }
            composable(Routes.DETAILS) {
                DetailsScreen(vm = idVm, language = language, onBack = { nav.popBackStack() })
            }
        }
    }
}

/** Barre de navigation Material 3 du design : 80 dp, pilule vert clair sur l'onglet actif. */
@Composable
private fun BottomBar(current: String, onSelect: (String) -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Ows.Surface)
            .drawBehind { drawLine(Ows.Border, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .navigationBarsPadding().height(80.dp),
    ) {
        TABS.forEach { tab ->
            val active = tab.route == current
            val color = if (active) Ows.GreenDark else Ows.Secondary
            Column(
                Modifier.weight(1f).fillMaxHeight().clickable(role = Role.Tab) { onSelect(tab.route) },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically),
            ) {
                Box(
                    Modifier.width(64.dp).height(32.dp).clip(RoundedCornerShape(16.dp))
                        .background(if (active) Ows.GreenSoft else Ows.Surface),
                    contentAlignment = Alignment.Center,
                ) { Icon(tab.icon, null, tint = color, modifier = Modifier.size(24.dp)) }
                Text(
                    stringResource(tab.label),
                    style = OwsType.Meta.copy(fontSize = 12.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium),
                    color = color,
                )
            }
        }
    }
}

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
