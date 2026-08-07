package com.starborn.mantracounter

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.starborn.mantracounter.data.Japa
import com.starborn.mantracounter.data.JapaDatabase
import com.starborn.mantracounter.ui.MantraApp
import com.starborn.mantracounter.ui.theme.MantraTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Walks into every screen, at the sizes most likely to break them. These are stability tests, not
 * appearance ones: they prove each screen composes, measures and lays out without throwing —
 * which is exactly where fixed dimensions and size arithmetic fail.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenFlowTest {

    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val japaName = "Hare Krishna Maha Mantra"

    private fun seed() {
        val context: Context = ApplicationProvider.getApplicationContext()
        runBlocking {
            val dao = JapaDatabase.get(context).japaDao()
            if (dao.allForExport().none { it.name == japaName }) {
                dao.insert(
                    Japa(
                        name = japaName,
                        deity = "Krishna",
                        count = 5_400,
                        malaSize = 108,
                        lifetimeTarget = 100_000,
                        createdAt = 1,
                        updatedAt = 1,
                    )
                )
            }
        }
    }

    private fun launch() {
        seed()
        compose.setContent { MantraTheme { MantraApp() } }
        compose.waitForIdle()
    }

    private fun openCounter(mode: String) {
        compose.onAllNodesWithText(japaName)[0].performClick()
        compose.waitForIdle()
        compose.onNodeWithText(mode).performClick()
        compose.waitForIdle()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `bead strand on the smallest phone`() {
        launch()
        openCounter("Bead strand")
        compose.onNodeWithText("Swipe down").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun `bead strand in landscape`() {
        launch()
        openCounter("Bead strand")
        compose.onNodeWithText("Swipe up").assertIsDisplayed()
    }

    /** The size arithmetic behind the big button is the thing most able to go negative here. */
    @Test
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun `tap mode in landscape`() {
        launch()
        openCounter("Tap buttons")
        compose.onNodeWithContentDescription("Remove one japa").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `tap mode on the smallest phone`() {
        launch()
        openCounter("Tap buttons")
        compose.onNodeWithContentDescription("Remove one japa").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w800dp-h1280dp-hdpi")
    fun `tap mode on a tablet`() {
        launch()
        openCounter("Tap buttons")
        compose.onNodeWithContentDescription("Remove one japa").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `timer mode on the smallest phone`() {
        launch()
        openCounter("Timer")
        compose.onNodeWithText("Interval").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun `timer mode in landscape`() {
        launch()
        openCounter("Timer")
        compose.onNodeWithText("Ends after").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `stats screen on the smallest phone`() {
        launch()
        compose.onNodeWithContentDescription("Stats").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("All japas").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xhdpi")
    fun `stats screen in landscape`() {
        launch()
        compose.onNodeWithContentDescription("Stats").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("All japas").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `settings screen on the smallest phone`() {
        launch()
        compose.onNodeWithContentDescription("More").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Settings").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Main screen background").assertIsDisplayed()
        // Further down the same scrolling column — present, just below the fold on a short screen.
        compose.onNodeWithText("Backup").assertExists()
        compose.onNodeWithText("Rishabh Dahiya").assertExists()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `archive screen on the smallest phone`() {
        launch()
        compose.onNodeWithContentDescription("More").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Archive").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Archive").assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "w320dp-h480dp-mdpi")
    fun `new japa editor on the smallest phone`() {
        launch()
        compose.onNodeWithText("New japa", useUnmergedTree = true).performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Deity").assertExists()
    }
}
