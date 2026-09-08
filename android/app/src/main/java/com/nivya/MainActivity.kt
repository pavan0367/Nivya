package com.nivya

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.nivya.core.navigation.AppNavGraph
import com.nivya.ui.theme.NivyaTheme

/**
 * Main Activity hosting the Jetpack Compose navigation architecture.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as NivyaApp
        val container = app.container

        setContent {
            NivyaTheme {
                AppNavGraph(appContainer = container)
            }
        }
    }
}
