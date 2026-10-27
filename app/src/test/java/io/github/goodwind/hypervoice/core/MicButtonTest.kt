package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MicButtonTest {
    @Test fun `the leanback speech orb is found by its id in any app`() {
        assertTrue(MicButton.matches("org.smarttube.stable:id/lb_search_bar_speech_orb", null))
        assertTrue(MicButton.matches("com.google.android.katniss:id/lb_search_bar_speech_orb", "Search Action"))
    }

    @Test fun `other ids that name a microphone or voice search button`() {
        assertTrue(MicButton.matches("x:id/speech_orb", null))
        assertTrue(MicButton.matches("x:id/voice_search_button", null))
        assertTrue(MicButton.matches("x:id/mic_button", null))
        assertTrue(MicButton.matches("x:id/btn_voice", null))
        assertTrue(MicButton.matches("x:id/microphone_icon", null))
    }

    @Test fun `ids that merely contain the letters are not microphones`() {
        assertFalse(MicButton.matches("x:id/dynamic_view", null))
        assertFalse(MicButton.matches("x:id/economic_chart", null))
        assertFalse(MicButton.matches("x:id/search_button", null))
        assertFalse(MicButton.matches("x:id/lb_search_text_editor", null))
        assertFalse(MicButton.matches("x:id/lb_search_frame", null))
    }

    @Test fun `descriptions in English and Russian that name voice input`() {
        for (d in listOf("Voice search", "Голосовой поиск", "Search by voice", "Microphone", "Микрофон", "Speak to search", "Говорите", "Голосовой ввод")) {
            assertTrue(d, MicButton.matches(null, d))
        }
    }

    @Test fun `plain search and microphone settings are not the button`() {
        for (d in listOf("Search", "Поиск", "Search Action", "Microphone settings", "Mute microphone", "Выключить микрофон", "Settings", "", "Настройки микрофона")) {
            assertFalse(d, MicButton.matches(null, d))
        }
    }

    @Test fun `nothing to go on is not a microphone`() {
        assertFalse(MicButton.matches(null, null))
        assertFalse(MicButton.matches("", ""))
    }
}
