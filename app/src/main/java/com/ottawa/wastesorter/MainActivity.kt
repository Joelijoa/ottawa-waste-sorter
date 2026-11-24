package com.ottawa.wastesorter

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.ottawa.wastesorter.navigation.AppNavHost
import com.ottawa.wastesorter.ui.theme.WasteSorterTheme

/** AppCompatActivity : nécessaire pour le changement de langue dans l'application (AppCompatDelegate). */
class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            WasteSorterTheme {
                AppNavHost()
            }
        }
    }
}
