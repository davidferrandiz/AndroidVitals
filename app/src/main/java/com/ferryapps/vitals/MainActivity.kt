package com.ferryapps.vitals

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.Scaffold
import com.ferryapps.vitals.feature.monitor.ui.MonitorScreen
import com.ferryapps.vitals.ui.theme.VitalsTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            VitalsTheme {
                Scaffold { innerPadding ->
                    MonitorScreen(contentPadding = innerPadding)
                }
            }
        }
    }
}
