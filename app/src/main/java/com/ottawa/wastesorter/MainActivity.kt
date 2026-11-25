package com.ottawa.wastesorter

import android.os.Bundle
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ottawa.wastesorter.navigation.AppNavHost
import com.ottawa.wastesorter.ui.theme.WasteSorterTheme

/** AppCompatActivity : nécessaire pour le changement de langue dans l'application (AppCompatDelegate). */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        // Le design est clair : icônes de barre d'état sombres, même si le téléphone est en mode sombre.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(android.graphics.Color.WHITE, android.graphics.Color.WHITE),
        )
        super.onCreate(savedInstanceState)
        setContent {
            WasteSorterTheme {
                AppNavHost()
            }
        }
    }
}
