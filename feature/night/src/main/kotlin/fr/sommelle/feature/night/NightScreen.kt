package fr.sommelle.feature.night

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.view.WindowManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import fr.sommelle.core.designsystem.SommelleColors
import fr.sommelle.core.designsystem.SommelleIcons
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin

/** Durée après laquelle l'écran se ferme seul si le repère de 20 min n'est pas activé. */
private const val AUTO_CLOSE_MILLIS = 90_000L

/** Durée de la règle du contrôle du stimulus (~20 minutes hors du lit, sans horloge). */
private const val STIMULUS_BELL_MILLIS = 20 * 60 * 1_000L

/** Luminosité de l'écran pendant l'affichage, très basse. */
private const val NIGHT_BRIGHTNESS = 0.05f

/**
 * Écran « Je n'arrive pas à dormir » (documentation/design.md, section 7) : presque noir et chaud,
 * aucune heure ni aucun chiffre (la barre d'état, qui affiche l'heure, est masquée),
 * luminosité abaissée, fermeture automatique ou repère sonore doux à 20 minutes (inspiré de CBT-i Coach).
 */
@Composable
fun NightScreen(onClose: () -> Unit) {
    val currentOnClose = rememberUpdatedState(onClose)
    val context = LocalContext.current
    val view = LocalView.current
    var bellState by remember { mutableStateOf(BellState.IDLE) }

    DisposableEffect(bellState) {
        val window = context.findActivity()?.window
        val previousBrightness = window?.attributes?.screenBrightness ?: WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        window?.let { w ->
            w.attributes = w.attributes.apply { screenBrightness = NIGHT_BRIGHTNESS }
            if (bellState == BellState.ACTIVE) {
                w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } else {
                w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
        controller?.hide(WindowInsetsCompat.Type.statusBars())
        onDispose {
            window?.let { w ->
                w.attributes = w.attributes.apply { screenBrightness = previousBrightness }
                w.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            controller?.show(WindowInsetsCompat.Type.statusBars())
        }
    }

    LaunchedEffect(bellState) {
        when (bellState) {
            BellState.IDLE -> {
                delay(AUTO_CLOSE_MILLIS)
                currentOnClose.value()
            }
            BellState.ACTIVE -> {
                withContext(Dispatchers.IO) { playGentleNightChime() }
                delay(STIMULUS_BELL_MILLIS)
                withContext(Dispatchers.IO) {
                    playGentleNightChime()
                    delay(1_800L)
                    playGentleNightChime()
                }
                bellState = BellState.DONE
            }
            BellState.DONE -> {
                delay(AUTO_CLOSE_MILLIS)
                currentOnClose.value()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SommelleColors.Ember)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 28.dp, vertical = 36.dp),
    ) {
        Column(
            modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(32.dp),
        ) {
            Icon(SommelleIcons.Moon, contentDescription = null, tint = SommelleColors.SandMuted, modifier = Modifier.size(44.dp))
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    stringResource(R.string.night_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = SommelleColors.Sand,
                    modifier = Modifier.semantics { heading() },
                )
                Text(stringResource(R.string.night_intro), style = MaterialTheme.typography.bodyLarge, color = SommelleColors.SandMuted)
            }
            Column(verticalArrangement = Arrangement.spacedBy(22.dp)) {
                NightStep(SommelleIcons.Door, stringResource(R.string.night_step_1))
                NightStep(SommelleIcons.Lamp, stringResource(R.string.night_step_2))
                NightStep(SommelleIcons.Bed, stringResource(R.string.night_step_3))
            }
            when (bellState) {
                BellState.IDLE -> {
                    TextButton(
                        onClick = { bellState = BellState.ACTIVE },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            stringResource(R.string.night_bell_start),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SommelleColors.SandMuted,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
                BellState.ACTIVE -> {
                    Text(
                        stringResource(R.string.night_bell_active),
                        style = MaterialTheme.typography.bodyMedium,
                        color = SommelleColors.Sand,
                    )
                }
                BellState.DONE -> {
                    Text(
                        stringResource(R.string.night_bell_done),
                        style = MaterialTheme.typography.bodyLarge,
                        color = SommelleColors.Sand,
                    )
                }
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 16.dp)) {
            OutlinedButton(
                onClick = onClose,
                modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, SommelleColors.Sand),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = SommelleColors.EmberSurface,
                    contentColor = SommelleColors.Sand,
                ),
            ) {
                Text(stringResource(R.string.night_ok), style = MaterialTheme.typography.labelLarge)
            }
            if (bellState != BellState.ACTIVE) {
                Text(
                    stringResource(R.string.night_auto_close),
                    style = MaterialTheme.typography.bodySmall,
                    color = SommelleColors.SandMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private enum class BellState { IDLE, ACTIVE, DONE }

@Composable
private fun NightStep(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(icon, contentDescription = null, tint = SommelleColors.Sand, modifier = Modifier.size(26.dp).padding(top = 2.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = SommelleColors.Sand)
    }
}

private fun playGentleNightChime() {
    runCatching {
        val sampleRate = 24_000
        val durationSec = 1.6
        val numSamples = (sampleRate * durationSec).toInt()
        val samples = ShortArray(numSamples)
        val fundamental = 432.0
        val overtone = 648.0
        for (i in 0 until numSamples) {
            val t = i.toDouble() / sampleRate
            val attack = (t / 0.05).coerceAtMost(1.0)
            val decay = exp(-2.4 * t)
            val wave = 0.75 * sin(2.0 * PI * fundamental * t) + 0.25 * sin(2.0 * PI * overtone * t)
            val value = (wave * attack * decay * 0.20 * Short.MAX_VALUE).toInt()
            samples[i] = value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(samples.size * 2)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()
        track.write(samples, 0, samples.size)
        track.play()
        Thread.sleep((durationSec * 1000).toLong() + 100L)
        track.release()
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
