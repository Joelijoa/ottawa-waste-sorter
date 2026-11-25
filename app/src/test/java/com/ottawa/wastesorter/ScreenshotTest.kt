package com.ottawa.wastesorter

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.core.view.drawToBitmap
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ottawa.wastesorter.domain.model.ItemCatalog
import com.ottawa.wastesorter.feature.guide.GuideDetailScreen
import com.ottawa.wastesorter.feature.guide.GuideScreen
import com.ottawa.wastesorter.feature.history.HistoryScreen
import com.ottawa.wastesorter.feature.home.HomeScreen
import com.ottawa.wastesorter.feature.identification.DetailsScreen
import com.ottawa.wastesorter.feature.identification.IdentificationViewModel
import com.ottawa.wastesorter.feature.identification.ResultScreen
import com.ottawa.wastesorter.feature.identification.UnknownScreen
import com.ottawa.wastesorter.feature.search.SearchScreen
import com.ottawa.wastesorter.feature.settings.SettingsScreen
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.ui.theme.Ows
import com.ottawa.wastesorter.ui.theme.WasteSorterTheme
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.junit.Before
import org.junit.Rule
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowNetworkCapabilities
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File

/**
 * Captures des principaux écrans, rendues sur la JVM par Robolectric (format du design : 412 × 892 dp).
 * Les PNG sont écrits dans app/build/screenshots et publiés avec l'APK par la CI.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w412dp-h892dp-xhdpi")
class ScreenshotTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Robolectric n'a pas de réseau par défaut : on simule une connexion Internet (sinon l'accueil passe en mode hors ligne). */
    @Before
    fun online() {
        val cm = ApplicationProvider.getApplicationContext<Context>().getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val caps = ShadowNetworkCapabilities.newInstance()
        shadowOf(caps).addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        shadowOf(cm).setNetworkCapabilities(cm.activeNetwork, caps)
    }

    private val container get() = ApplicationProvider.getApplicationContext<WasteSorterApplication>().container

    private fun shot(name: String, content: @Composable () -> Unit) {
        compose.setContent {
            WasteSorterTheme { Box(Modifier.fillMaxSize().background(Ows.Background)) { content() } }
        }
        compose.waitForIdle()
        // Dessin direct de la vue racine (le mode PixelCopy de Robolectric attend un rendu matériel).
        val bitmap = compose.activity.window.decorView.rootView.drawToBitmap()
        val dir = File(System.getProperty("screenshots.dir") ?: "build/screenshots").apply { mkdirs() }
        File(dir, "$name.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    private fun vmWith(itemId: String): IdentificationViewModel = IdentificationViewModel(container).apply {
        classifyCatalog(ItemCatalog.byId(itemId)!!, "en")
        consumeEvent()
    }

    @Test fun s01_home() = shot("01_home") {
        HomeScreen({}, {}, {}, {}, {}, {})
    }

    @Test fun s05_result_biscuit() = shot("05_result_biscuit") {
        ResultScreen(vmWith("biscuit"), "en", {}, {}, {}, {})
    }

    @Test fun s05b_result_cup() = shot("05b_result_coffee_cup") {
        ResultScreen(vmWith("cup"), "en", {}, {}, {}, {})
    }

    @Test fun s06_multi_pringles() = shot("06_multi_pringles") {
        ResultScreen(vmWith("pringles"), "en", {}, {}, {}, {})
    }

    @Test fun s07_search() = shot("07_search") {
        SearchScreen("en", {}, {}, { _, _ -> }, {})
    }

    @Test fun s08_details() = shot("08_details") {
        val vm = IdentificationViewModel(container).apply {
            val item = ItemCatalog.byId("biscuit")!!
            startDetails(item.name.en, catalog = item)
        }
        DetailsScreen(vm, "en", {})
    }

    @Test fun s09_history_empty() = shot("09_history_empty") {
        HistoryScreen({}, {})
    }

    @Test fun s10_guide() = shot("10_guide") {
        GuideScreen("en", {})
    }

    @Test fun s10b_guide_black() = shot("10b_guide_black") {
        GuideDetailScreen(Destination.BLACK_BIN, "en", {})
    }

    @Test fun s10c_guide_garbage() = shot("10c_guide_garbage") {
        GuideDetailScreen(Destination.GARBAGE, "en", {})
    }

    @Test fun s11_settings() = shot("11_settings") {
        SettingsScreen()
    }

    @Test fun s1_unknown() = shot("S1_unknown") {
        UnknownScreen({}, {}, {})
    }

    @Config(qualifiers = "+fr")
    @Test fun fr_result_biscuit() = shot("fr_05_result_biscuit") {
        val vm = IdentificationViewModel(container).apply {
            classifyCatalog(ItemCatalog.byId("biscuit")!!, "fr")
            consumeEvent()
        }
        ResultScreen(vm, "fr", {}, {}, {}, {})
    }
}
