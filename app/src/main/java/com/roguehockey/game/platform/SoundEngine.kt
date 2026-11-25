package com.roguehockey.game.platform

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.roguehockey.game.engine.SoundEvent
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.sin
import kotlin.random.Random

class SoundEngine(context: Context) {

    @Volatile
    var muted: Boolean = false

    @Volatile
    var hapticsEnabled: Boolean = true

    private val sampleRate = 22050
    private val executor = Executors.newFixedThreadPool(2)

    private val vibrator: Vibrator? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }

    fun play(event: SoundEvent, comboStep: Int = 1) {
        if (hapticsEnabled) triggerHaptic(event)
        if (muted) return

        val pitchBoost = 1f + (comboStep.coerceIn(1, 10) - 1) * 0.06f
        executor.execute {
            runCatching {
                val samples = when (event) {
                    SoundEvent.FLICK_LAUNCH -> synthSweep(210f, 440f, 0.085f, 0.35f)
                    SoundEvent.WALL_BOUNCE -> synthPercussive(260f, 0.045f, 0.25f, noiseMix = 0.22f)
                    SoundEvent.CHIP_HIT -> synthTone(540f * pitchBoost, 0.075f, 0.32f)
                    SoundEvent.MULT_HIT -> synthChord(listOf(660f * pitchBoost, 990f * pitchBoost), 0.11f, 0.30f)
                    SoundEvent.SPINNER_HIT -> synthSweep(480f, 920f, 0.095f, 0.28f)
                    SoundEvent.JACKPOT_HIT -> synthChord(listOf(784f, 988f, 1175f), 0.17f, 0.35f)
                    SoundEvent.OBSTACLE_CRACK -> synthPercussive(380f, 0.06f, 0.32f, noiseMix = 0.55f)
                    SoundEvent.OBSTACLE_SHATTER -> synthPercussive(220f, 0.13f, 0.42f, noiseMix = 0.72f)
                    SoundEvent.BARREL_EXPLODE -> synthExplosion(0.24f, 0.52f)
                    SoundEvent.POWERUP_PICKUP -> synthArpeggio(listOf(523f, 659f, 784f, 1046f), 0.19f, 0.32f)
                    SoundEvent.GOAL_SCORE -> synthArpeggio(listOf(587f, 880f, 1175f), 0.26f, 0.42f)
                    SoundEvent.SHOT_BANKED -> synthChord(listOf(440f, 554f, 659f), 0.16f, 0.30f)
                    SoundEvent.ROUND_CLEAR -> synthArpeggio(listOf(523f, 659f, 784f, 1046f, 1318f), 0.38f, 0.40f)
                    SoundEvent.SHOP_BUY -> synthArpeggio(listOf(740f, 988f), 0.12f, 0.30f)
                    SoundEvent.GAME_OVER -> synthArpeggio(listOf(392f, 330f, 262f), 0.35f, 0.35f)
                }
                playPcm(samples)
            }
        }
    }

    private fun triggerHaptic(event: SoundEvent) {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return
        val (durationMs, amplitude) = when (event) {
            SoundEvent.FLICK_LAUNCH -> 14L to 90
            SoundEvent.OBSTACLE_SHATTER -> 24L to 145
            SoundEvent.BARREL_EXPLODE -> 55L to 230
            SoundEvent.GOAL_SCORE -> 45L to 200
            SoundEvent.POWERUP_PICKUP -> 22L to 120
            SoundEvent.JACKPOT_HIT -> 24L to 140
            SoundEvent.ROUND_CLEAR -> 60L to 180
            else -> return
        }
        runCatching {
            vib.vibrate(VibrationEffect.createOneShot(durationMs, amplitude))
        }
    }

    private fun synthTone(freq: Float, durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 24f)
            val s = sin(2.0 * PI * freq * t).toFloat() * env * volume
            out[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthSweep(startFreq: Float, endFreq: Float, durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        var phase = 0.0
        for (i in 0 until count) {
            val frac = i.toFloat() / count
            val freq = startFreq + (endFreq - startFreq) * frac
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - frac) * (1f - frac)
            val s = sin(phase).toFloat() * env * volume
            out[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthChord(freqs: List<Float>, durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val invN = 1f / freqs.size
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 14f)
            var sum = 0f
            for (f in freqs) {
                sum += sin(2.0 * PI * f * t).toFloat()
            }
            val s = sum * invN * env * volume
            out[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthArpeggio(freqs: List<Float>, durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val noteLen = (count / freqs.size).coerceAtLeast(1)
        for (i in 0 until count) {
            val noteIdx = (i / noteLen).coerceAtMost(freqs.lastIndex)
            val localT = (i % noteLen).toFloat() / sampleRate
            val env = exp(-localT * 18f)
            val s = sin(2.0 * PI * freqs[noteIdx] * localT).toFloat() * env * volume
            out[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthPercussive(freq: Float, durationSec: Float, volume: Float, noiseMix: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val rng = Random(42)
        for (i in 0 until count) {
            val t = i.toFloat() / sampleRate
            val env = exp(-t * 32f)
            val tone = sin(2.0 * PI * freq * t).toFloat()
            val noise = rng.nextFloat() * 2f - 1f
            val s = (tone * (1f - noiseMix) + noise * noiseMix) * env * volume
            out[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun synthExplosion(durationSec: Float, volume: Float): ShortArray {
        val count = (sampleRate * durationSec).toInt().coerceAtLeast(1)
        val out = ShortArray(count)
        val rng = Random(99)
        var phase = 0.0
        for (i in 0 until count) {
            val frac = i.toFloat() / count
            val freq = 140f * (1f - frac * 0.7f)
            phase += 2.0 * PI * freq / sampleRate
            val env = (1f - frac) * (1f - frac)
            val noise = (rng.nextFloat() * 2f - 1f) * 0.55f
            val bass = sin(phase).toFloat() * 0.65f
            val s = (bass + noise) * env * volume
            out[i] = (s.coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun playPcm(samples: ShortArray) {
        val minBuf = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        val bufBytes = maxOf(minBuf, samples.size * 2)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufBytes)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        track.write(samples, 0, samples.size)
        track.play()
        val durationMs = (samples.size * 1000L / sampleRate) + 40L
        Thread.sleep(durationMs)
        track.stop()
        track.release()
    }

    fun release() {
        executor.shutdownNow()
    }
}
