package fr.sommelle.feature.cognitive

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale
import kotlin.math.exp
import kotlin.math.PI
import kotlin.math.sin

/**
 * Guide vocal hors ligne pour écouter un exercice les yeux fermés :
 * synthèse vocale du téléphone à débit ralenti, ponctuée d'une cloche acoustique douce
 * générée mathématiquement (aucun fichier audio embarqué, aucune connexion réseau).
 */
internal class VoiceExerciseGuide(
    context: Context,
    private val onFinished: () -> Unit,
) {
    private var tts: TextToSpeech? = null
    private var ready = false
    private var pendingRun: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.setSpeechRate(0.86f)
                tts?.setPitch(0.95f)
                ready = true
                pendingRun?.invoke()
                pendingRun = null
            } else {
                onFinished()
            }
        }
    }

    fun start(intro: String, steps: List<String>, pauseMsBetweenSteps: Long) {
        val action = {
            playGentleChime()
            val engine = tts
            if (engine != null) {
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = Unit
                    override fun onError(utteranceId: String?) {
                        onFinished()
                    }
                    override fun onDone(utteranceId: String?) {
                        if (utteranceId == "end") {
                            playGentleChime()
                            onFinished()
                        }
                    }
                })
                engine.speak(intro, TextToSpeech.QUEUE_FLUSH, null, "intro")
                steps.forEachIndexed { index, step ->
                    engine.playSilentUtterance(1800L, TextToSpeech.QUEUE_ADD, "pre_$index")
                    engine.speak(step, TextToSpeech.QUEUE_ADD, null, "step_$index")
                    val id = if (index == steps.lastIndex) "end" else "pause_$index"
                    engine.playSilentUtterance(pauseMsBetweenSteps, TextToSpeech.QUEUE_ADD, id)
                }
            }
        }
        if (ready) action() else pendingRun = action
    }

    fun stop() {
        pendingRun = null
        runCatching { tts?.stop() }
    }

    fun release() {
        stop()
        runCatching { tts?.shutdown() }
        tts = null
    }

    /** Cloche harmonique douce à 432 Hz avec décroissance exponentielle (0,9 seconde). */
    private fun playGentleChime() {
        Thread {
            runCatching {
                val sampleRate = 22050
                val durationSec = 0.9
                val numSamples = (sampleRate * durationSec).toInt()
                val samples = ShortArray(numSamples)
                for (i in 0 until numSamples) {
                    val t = i.toDouble() / sampleRate
                    val envelope = exp(-4.2 * t)
                    val wave = 0.7 * sin(2.0 * PI * 432.0 * t) + 0.3 * sin(2.0 * PI * 864.0 * t)
                    samples[i] = (wave * envelope * Short.MAX_VALUE * 0.22).toInt().toShort()
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
                Thread.sleep(950)
                track.release()
            }
        }.start()
    }
}
