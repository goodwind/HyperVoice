package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutcomeTest {
    @Test fun `each failure maps to the speech recogniser error a client expects`() {
        assertEquals(7, Outcome.NoSpeech.recognizerError())      // ERROR_NO_MATCH
        assertEquals(9, Outcome.Unauthorized.recognizerError())  // ERROR_INSUFFICIENT_PERMISSIONS
        assertEquals(9, Outcome.NoKey.recognizerError())
        assertEquals(4, Outcome.RateLimited.recognizerError())   // ERROR_SERVER
        assertEquals(4, Outcome.Server(500).recognizerError())
        assertEquals(2, Outcome.Network("x").recognizerError())  // ERROR_NETWORK
        assertEquals(6, Outcome.Timeout.recognizerError())       // ERROR_SPEECH_TIMEOUT
        assertEquals(3, Outcome.NoAudio.recognizerError())       // ERROR_AUDIO
    }

    @Test fun `a transcript is not an error`() {
        assertNull(Outcome.Text("hi").recognizerError())
    }
}
