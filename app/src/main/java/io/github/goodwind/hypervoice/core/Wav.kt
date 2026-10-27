package io.github.goodwind.hypervoice.core

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** 16 bit mono PCM in a WAV container, and the little silences speech recognisers like around short clips. */
object Wav {
    private const val HEADER = 44

    fun encode(pcm: ByteArray, sampleRate: Int): ByteArray {
        val h = ByteBuffer.allocate(HEADER).order(ByteOrder.LITTLE_ENDIAN)
        h.put("RIFF".toByteArray()).putInt(36 + pcm.size).put("WAVE".toByteArray())
        h.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
        h.putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        h.put("data".toByteArray()).putInt(pcm.size)
        return h.array() + pcm
    }

    fun silence(ms: Int, sampleRate: Int): ByteArray = ByteArray(sampleRate * ms / 1000 * 2)

    /** Whisper dropped a one second clip that began right on the first word; a little room each side fixes it. */
    fun pad(pcm: ByteArray, sampleRate: Int, beforeMs: Int, afterMs: Int): ByteArray =
        silence(beforeMs, sampleRate) + pcm + silence(afterMs, sampleRate)

    fun durationMs(pcmBytes: Int, sampleRate: Int): Long = pcmBytes / 2 * 1000L / sampleRate
}
