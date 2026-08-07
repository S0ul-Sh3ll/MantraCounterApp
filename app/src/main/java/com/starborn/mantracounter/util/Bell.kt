package com.starborn.mantracounter.util

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.starborn.mantracounter.R

/**
 * The app's two sounds: a bell for a mala closing or a timer tick, and a chanted Om to close a
 * timed session. SoundPool rather than MediaPlayer — both have to start the instant they are
 * called for, and may need to overlap.
 */
class SoundPlayer(context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        )
        .build()

    private val loaded = mutableSetOf<Int>()
    private val bellId: Int
    private val omId: Int

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(loaded) { loaded.add(sampleId) }
        }
        bellId = pool.load(context.applicationContext, R.raw.bell, 1)
        omId = pool.load(context.applicationContext, R.raw.om, 1)
    }

    fun playBell() = play(bellId)

    /** The closing sound of a timed session. */
    fun playOm() = play(omId)

    private fun play(id: Int) {
        if (synchronized(loaded) { id !in loaded }) return
        pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    fun release() = pool.release()
}

@Composable
fun rememberSoundPlayer(): SoundPlayer {
    val context = LocalContext.current
    val player = remember { SoundPlayer(context) }
    DisposableEffect(player) { onDispose { player.release() } }
    return player
}
