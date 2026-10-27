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

    @Test fun `a short sentence for the person to read on the screen`() {
        assertEquals("Нет ключа Groq", Outcome.NoKey.message())
        assertEquals("Ключ Groq не принят", Outcome.Unauthorized.message())
        assertEquals("Не расслышал", Outcome.NoSpeech.message())
        assertEquals("Нет связи с Groq", Outcome.Network("x").message())
        assertEquals("Лимит Groq исчерпан", Outcome.RateLimited.message())
        assertEquals("Ошибка Groq 500", Outcome.Server(500).message())
        assertEquals("Не услышал", Outcome.Timeout.message())
        assertEquals("Нет доступа к микрофону", Outcome.NoAudio.message())
    }
}
