package com.ottawa.wastesorter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ottawa.wastesorter.feature.guide.GuideDetailScreen
import com.ottawa.wastesorter.feature.guide.GuideScreen
import com.ottawa.wastesorter.rules.Destination
import com.ottawa.wastesorter.ui.theme.WasteSorterTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Le guide fonctionne hors ligne : il est généré à partir des règles embarquées. (Appareil en anglais.) */
@RunWith(AndroidJUnit4::class)
class GuideScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun guideListsTheFourCurbsideStreams() {
        var opened: Destination? = null
        compose.setContent { WasteSorterTheme { GuideScreen(language = "en", onOpenBin = { opened = it }) } }
        compose.onNodeWithText("Garbage").assertIsDisplayed()
        compose.onNodeWithText("Black Recycling Bin").assertIsDisplayed()
        compose.onNodeWithText("Blue Recycling Bin").performClick()
        assertEquals(Destination.BLUE_BIN, opened)
    }

    @Test
    fun blackBinDetailSendsCupsAndSpiralCansToTheBlueBin() {
        compose.setContent { WasteSorterTheme { GuideDetailScreen(Destination.BLACK_BIN, "en", onBack = {}) } }
        compose.onNodeWithText("Paper cups and spiral cans").assertIsDisplayed()
        compose.onNodeWithText("Blue bin (since January 2026)").assertIsDisplayed()
    }
}
