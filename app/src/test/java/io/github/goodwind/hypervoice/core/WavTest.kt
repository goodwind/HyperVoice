package io.github.goodwind.hypervoice.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WavTest {
    private fun header(wav: ByteArray) = ByteBuffer.wrap(wav).order(ByteOrder.LITTLE_ENDIAN)

    @Test fun `a wav has the 44 byte header of 16 bit mono pcm and then the samples`() {
        val pcm = byteArrayOf(1, 2, 3, 4, 5, 6)
        val wav = Wav.encode(pcm, 16000)
        val h = header(wav)
        assertEquals(44 + pcm.size, wav.size)
        assertEquals("RIFF", String(wav, 0, 4))
        assertEquals(36 + pcm.size, h.getInt(4))
        assertEquals("WAVEfmt ", String(wav, 8, 8))
        assertEquals(16, h.getInt(16))
        assertEquals(1.toShort(), h.getShort(20)) // pcm
        assertEquals(1.toShort(), h.getShort(22)) // mono
        assertEquals(16000, h.getInt(24))
        assertEquals(32000, h.getInt(28)) // bytes a second
        assertEquals(2.toShort(), h.getShort(32))
        assertEquals(16.toShort(), h.getShort(34))
        assertEquals("data", String(wav, 36, 4))
        assertEquals(pcm.size, h.getInt(40))
        assertTrue(pcm.contentEquals(wav.copyOfRange(44, wav.size)))
    }

    @Test fun `silence is zeros of the right length for the rate`() {
        val s = Wav.silence(300, 16000)
        assertEquals(9600, s.size) // 0.3 s of 16 bit samples
        assertTrue(s.all { it == 0.toByte() })
    }

    @Test fun `padding puts silence before and after the speech`() {
        val speech = byteArrayOf(9, 9)
        val padded = Wav.pad(speech, 16000, beforeMs = 100, afterMs = 200)
        assertEquals(3200 + 2 + 6400, padded.size)
        assertEquals(9.toByte(), padded[3200])
        assertEquals(0.toByte(), padded[0])
        assertEquals(0.toByte(), padded.last())
    }

    @Test fun `the length of speech in milliseconds`() {
        assertEquals(1000L, Wav.durationMs(32000, 16000))
        assertEquals(0L, Wav.durationMs(0, 16000))
    }
}
