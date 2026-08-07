package com.starborn.mantracounter

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import com.starborn.mantracounter.ui.MantraApp
import com.starborn.mantracounter.ui.theme.MantraTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The app rendered at a spread of real device sizes. This does not judge how it looks — it proves
 * every screen composes and measures without throwing, which is where fixed sizes and negative
 * arithmetic blow up.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenSizeTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun renderApp() {
        compose.setContent { MantraTheme { MantraApp() } }
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `smallest supported phone`() {
        renderApp()
        compose.onNodeWithText("Mantra Counter").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun `common phone`() {
        renderApp()
        compose.onNodeWithText("Mantra Counter").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w411dp-h891dp-xxhdpi")
    fun `large phone`() {
        renderApp()
        compose.onNodeWithText("Mantra Counter").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun `phone in landscape`() {
        renderApp()
        compose.onNodeWithText("Mantra Counter").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-hdpi")
    fun `tablet`() {
        renderApp()
        compose.onNodeWithText("Mantra Counter").assertIsDisplayed()
    }

    /**
     * The largest accessibility font setting, on a normal phone. Text at twice the size is what
     * pushes fixed-height rows past their bounds.
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun `largest accessibility font size`() {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = 2f)
            ) {
                MantraTheme { MantraApp() }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Mantra Counter").assertIsDisplayed()
    }

    /** A small phone with large text — the tightest combination the app has to survive. */
    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `smallest phone with large font`() {
        compose.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density = base.density, fontScale = 1.6f)
            ) {
                MantraTheme { MantraApp() }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("Mantra Counter").assertIsDisplayed()
    }
}
