package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndpointingTest {
    private fun e() = Endpointing(startTimeoutMs = 8000, silenceMs = 500, maxMs = 20000)

    @Test fun `nothing happens while waiting inside the start window`() {
        val ep = e()
        ep.begin(0)
        assertNull(ep.tick(7999))
    }

    @Test fun `no audio at all in the start window is a timeout, once`() {
        val ep = e()
        ep.begin(0)
        assertEquals(Endpointing.Event.Timeout, ep.tick(8000))
        assertNull(ep.tick(9000))
    }

    @Test fun `the first audio is the start of speech, once`() {
        val ep = e()
        ep.begin(0)
        assertEquals(Endpointing.Event.SpeechStarted, ep.onAudio(1200))
        assertNull(ep.onAudio(1300))
    }

    @Test fun `audio after the start window began still counts`() {
        val ep = e()
        ep.begin(0)
        assertEquals(Endpointing.Event.SpeechStarted, ep.onAudio(7000))
        assertNull(ep.tick(7400)) // the timeout no longer applies once speech started
    }

    @Test fun `a gap after audio ends the speech`() {
        val ep = e()
        ep.begin(0)
        ep.onAudio(1000)
        ep.onAudio(1500)
        assertNull(ep.tick(1999))
        assertEquals(Endpointing.Event.SpeechEnded, ep.tick(2000))
        assertNull(ep.tick(3000))
    }

    @Test fun `speech that goes on too long is cut`() {
        val ep = e()
        ep.begin(0)
        ep.onAudio(1000)
        var last: Endpointing.Event? = null
        var t = 1000L
        while (t < 21500 && last != Endpointing.Event.TooLong) { t += 100; ep.onAudio(t); last = ep.tick(t) }
        assertEquals(Endpointing.Event.TooLong, last)
    }

    @Test fun `finishing by hand ends the speech if it had started`() {
        val ep = e()
        ep.begin(0)
        ep.onAudio(1000)
        assertEquals(Endpointing.Event.SpeechEnded, ep.finish())
        assertNull(ep.finish())
    }

    @Test fun `finishing by hand before any audio is a timeout`() {
        val ep = e()
        ep.begin(0)
        assertEquals(Endpointing.Event.Timeout, ep.finish())
    }
}
