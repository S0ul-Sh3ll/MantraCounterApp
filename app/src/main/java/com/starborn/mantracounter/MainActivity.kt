package com.starborn.mantracounter

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.starborn.mantracounter.ui.MantraApp
import com.starborn.mantracounter.ui.theme.MantraTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Japa is done with the phone in hand and barely touched — sometimes not touched at all,
        // in timer mode. The screen turning itself off mid-practice is the thing to avoid, so the
        // flag is held for as long as the app is in front rather than only on the counter screen.
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        enableEdgeToEdge()
        setContent {
            MantraTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background,
                ) {
                    MantraApp()
                }
            }
        }
    }
}
