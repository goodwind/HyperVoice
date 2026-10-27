package io.github.goodwind.hypervoice.core

import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

/** Where audio comes from: the remote's microphone on the TV, a script in the tests. */
interface AudioSource {
    /** Opens the microphone. Throws if it cannot be had. */
    fun start()

    /** Reads what is there without waiting; the number of samples, 0 when there is none yet. */
    fun read(buffer: ShortArray): Int

    fun stop()
}

/** Turns a WAV clip into text. */
fun interface Transcriber {
    fun transcribe(wav: ByteArray): Outcome
}

/**
 * One phrase, from opening the microphone to the text: listen, find where the phrase ends, send it, report.
 * [run] blocks, so the caller puts it on its own thread; [finish] and [cancel] may come from any thread.
 */
class VoiceSession(
    private val source: AudioSource,
    private val transcriber: Transcriber,
    private val listener: Listener,
    private val now: () -> Long,
    private val sleep: (Long) -> Unit,
    private val sampleRate: Int = 16_000,
    private val startTimeoutMs: Long = 8_000,
    private val silenceMs: Long = 500,
    private val maxMs: Long = 20_000,
    private val minSpeechMs: Long = 250,
    private val roomMs: Int = 300,
) {
    interface Listener {
        fun onReady()
        fun onSpeechStarted()
        fun onLevel(rms: Double)
        fun onSpeechEnded()
        fun onResult(outcome: Outcome)
    }

    @Volatile private var finishRequested = false
    @Volatile private var cancelled = false

    /** The phrase is over (the button was let go): do not wait for a gap. */
    fun finish() { finishRequested = true }

    /** Nobody wants the result any more: report nothing. */
    fun cancel() { cancelled = true }

    fun run() {
        try {
            source.start()
        } catch (e: Exception) {
            listener.onResult(Outcome.NoAudio)
            return
        }
        listener.onReady()
        val endpointing = Endpointing(startTimeoutMs, silenceMs, maxMs)
        endpointing.begin(now())
        val pcm = ByteArrayOutputStream()
        val buffer = ShortArray(sampleRate / 10)
        var event: Endpointing.Event? = null
        try {
            while (event == null) {
                if (cancelled) return
                val n = source.read(buffer)
                val t = now()
                if (n > 0) {
                    append(pcm, buffer, n)
                    listener.onLevel(rms(buffer, n))
                    if (endpointing.onAudio(t) == Endpointing.Event.SpeechStarted) listener.onSpeechStarted()
                } else {
                    sleep(20)
                }
                if (cancelled) return
                event = if (finishRequested) endpointing.finish() else endpointing.tick(t)
            }
        } finally {
            source.stop()
        }
        if (event == Endpointing.Event.Timeout) {
            listener.onResult(Outcome.Timeout)
            return
        }
        listener.onSpeechEnded()
        val speech = pcm.toByteArray()
        if (Wav.durationMs(speech.size, sampleRate) < minSpeechMs) {
            listener.onResult(Outcome.NoSpeech)
            return
        }
        val outcome = transcriber.transcribe(Wav.encode(Wav.pad(speech, sampleRate, roomMs, roomMs), sampleRate))
        if (!cancelled) listener.onResult(outcome)
    }

    private fun append(out: ByteArrayOutputStream, samples: ShortArray, n: Int) {
        for (i in 0 until n) {
            val s = samples[i].toInt()
            out.write(s and 0xFF)
            out.write(s shr 8 and 0xFF)
        }
    }

    private fun rms(samples: ShortArray, n: Int): Double {
        var sum = 0.0
        for (i in 0 until n) sum += samples[i].toDouble() * samples[i]
        return sqrt(sum / n)
    }
}
