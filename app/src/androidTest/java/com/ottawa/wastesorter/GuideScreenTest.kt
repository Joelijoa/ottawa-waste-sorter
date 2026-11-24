package com.ottawa.wastesorter

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ottawa.wastesorter.feature.guide.GuideScreen
import com.ottawa.wastesorter.ui.theme.WasteSorterTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Le guide fonctionne hors ligne : il est généré à partir des règles embarquées. */
@RunWith(AndroidJUnit4::class)
class GuideScreenTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun guideShowsBlueBinAndExpandsExamples() {
        compose.setContent { WasteSorterTheme { GuideScreen(language = "en") } }
        compose.onNodeWithText("Blue bin — containers").assertIsDisplayed().performClick()
        compose.onNodeWithText("Aluminum foil").assertIsDisplayed()
    }

    @Test
    fun searchFindsPringlesInBlueBin() {
        compose.setContent { WasteSorterTheme { GuideScreen(language = "en") } }
        compose.onNodeWithTag("guide_list").assertIsDisplayed()
        compose.onNodeWithText("Search an item").performTextInput("Pringles")
        compose.onNodeWithText("Blue bin — containers").assertIsDisplayed()
    }
}
