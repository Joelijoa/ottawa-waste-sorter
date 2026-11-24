package com.ottawa.wastesorter.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ottawa.wastesorter.R
import com.ottawa.wastesorter.appContainer
import com.ottawa.wastesorter.feature.camera.CameraScreen
import com.ottawa.wastesorter.feature.guide.GuideScreen
import com.ottawa.wastesorter.feature.history.HistoryScreen
import com.ottawa.wastesorter.feature.home.HomeScreen
import com.ottawa.wastesorter.feature.identification.IdentificationViewModel
import com.ottawa.wastesorter.feature.identification.ManualSelectionScreen
import com.ottawa.wastesorter.feature.identification.PhotoPreviewScreen
import com.ottawa.wastesorter.feature.identification.ResultScreen
import com.ottawa.wastesorter.feature.settings.SettingsScreen

object Routes {
    const val HOME = "home"
    const val HISTORY = "history"
    const val GUIDE = "guide"
    const val SETTINGS = "settings"
    const val CAMERA = "camera"
    const val PREVIEW = "preview"
    const val MANUAL = "manual"
    const val RESULT = "result"
}

private data class TopLevel(val route: String, @StringRes val label: Int, val icon: ImageVector)

private val TOP_LEVEL = listOf(
    TopLevel(Routes.HOME, R.string.nav_home, Icons.Filled.Home),
    TopLevel(Routes.HISTORY, R.string.nav_history, Icons.Filled.History),
    TopLevel(Routes.GUIDE, R.string.nav_guide, Icons.AutoMirrored.Filled.MenuBook),
    TopLevel(Routes.SETTINGS, R.string.nav_settings, Icons.Filled.Settings),
)

@Composable
fun AppNavHost() {
    val nav = rememberNavController()
    val context = LocalContext.current
    // Portée activité : partagé entre caméra, aperçu, tri manuel et résultat.
    val idVm: IdentificationViewModel = viewModel(factory = IdentificationViewModel.factory(context.appContainer))
    val idState by idVm.state.collectAsStateWithLifecycle()
    val language = LocalConfiguration.current.locales[0].language

    val backStack by nav.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            idVm.setPhoto(uri)
            nav.navigate(Routes.PREVIEW) { launchSingleTop = true }
        }
    }
    val openGallery = { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }

    LaunchedEffect(idState.showResult) {
        if (idState.showResult) {
            if (nav.currentDestination?.route != Routes.RESULT) {
                nav.navigate(Routes.RESULT) {
                    // Le résultat remplace l'aperçu / le tri manuel dans la pile.
                    popUpTo(Routes.HOME)
                    launchSingleTop = true
                }
            }
            idVm.onResultShown()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (TOP_LEVEL.any { it.route == currentRoute }) {
                NavigationBar {
                    TOP_LEVEL.forEach { item ->
                        NavigationBarItem(
                            selected = currentRoute == item.route,
                            onClick = { nav.navigateTopLevel(item.route) },
                            icon = { Icon(item.icon, contentDescription = null) },
                            label = { Text(stringResource(item.label)) },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(nav, startDestination = Routes.HOME, modifier = Modifier.padding(innerPadding)) {
            composable(Routes.HOME) {
                HomeScreen(
                    onTakePhoto = { nav.navigate(Routes.CAMERA) },
                    onUploadPhoto = openGallery,
                    onManual = { nav.navigate(Routes.MANUAL) },
                    onGuide = { nav.navigateTopLevel(Routes.GUIDE) },
                    onOpenHistoryItem = { idVm.showOutcome(it.outcome) },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal)),
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(onOpen = { idVm.showOutcome(it.outcome) })
            }
            composable(Routes.GUIDE) { GuideScreen(language = language) }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.CAMERA) {
                CameraScreen(
                    onPhotoCaptured = { uri ->
                        idVm.setPhoto(uri)
                        nav.navigate(Routes.PREVIEW) { popUpTo(Routes.CAMERA) { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.PREVIEW) {
                PhotoPreviewScreen(
                    vm = idVm,
                    language = language,
                    onRetake = {
                        idVm.discardPhoto()
                        nav.navigate(Routes.CAMERA) { popUpTo(Routes.PREVIEW) { inclusive = true } }
                    },
                    onManual = { idVm.clearError(); nav.navigate(Routes.MANUAL) },
                    onOpenSettings = { idVm.clearError(); nav.navigateTopLevel(Routes.SETTINGS) },
                    onBack = { idVm.discardPhoto(); nav.popBackStack() },
                )
            }
            composable(Routes.MANUAL) {
                ManualSelectionScreen(
                    language = language,
                    onClassify = { name, type, components -> idVm.classifyManually(name, type, components) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.RESULT) {
                ResultScreen(
                    vm = idVm,
                    language = language,
                    onManual = { idVm.clearError(); nav.navigate(Routes.MANUAL) },
                    onRetake = {
                        idVm.discardPhoto()
                        nav.navigate(Routes.CAMERA) { popUpTo(Routes.HOME) }
                    },
                    onDone = {
                        idVm.discardPhoto()
                        if (!nav.popBackStack()) nav.navigateTopLevel(Routes.HOME)
                    },
                )
            }
        }
    }
}

private fun NavHostController.navigateTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
