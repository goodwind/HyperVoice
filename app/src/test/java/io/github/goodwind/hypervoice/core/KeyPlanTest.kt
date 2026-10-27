package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyPlanTest {
    private fun decide(
        recognitionActive: Boolean = false,
        fieldFocused: Boolean = false,
        pressMicOption: Boolean = true,
        micButtonFound: Boolean = false,
        openSearchOption: Boolean = false,
        searchButtonFound: Boolean = false,
        hasKey: Boolean = true,
    ) = KeyPlan.decide(recognitionActive, fieldFocused, pressMicOption, micButtonFound, openSearchOption, searchButtonFound, hasKey)

    @Test fun `an app that is already listening has the button`() {
        assertEquals(KeyPlan.Action.Ignore, decide(recognitionActive = true, fieldFocused = true))
    }

    @Test fun `a field in focus is dictated into`() {
        assertEquals(KeyPlan.Action.Dictate, decide(fieldFocused = true))
    }

    @Test fun `nothing on screen to speak to, nothing at all happens`() {
        assertEquals(KeyPlan.Action.Ignore, decide())
        assertEquals(KeyPlan.Action.Ignore, decide(hasKey = false)) // not even a nag about the key
    }

    @Test fun `no field but a microphone button, it is pressed`() {
        assertEquals(KeyPlan.Action.PressMicButton, decide(micButtonFound = true))
    }

    @Test fun `the option to press microphone buttons off, nothing happens`() {
        assertEquals(KeyPlan.Action.Ignore, decide(pressMicOption = false, micButtonFound = true))
    }

    @Test fun `a search button is pressed only when asked for, and when there is no microphone button`() {
        assertEquals(KeyPlan.Action.OpenSearch, decide(openSearchOption = true, searchButtonFound = true))
        assertEquals(KeyPlan.Action.Ignore, decide(openSearchOption = false, searchButtonFound = true))
        assertEquals(KeyPlan.Action.PressMicButton, decide(micButtonFound = true, openSearchOption = true, searchButtonFound = true))
    }

    @Test fun `without a key nothing is opened or pressed for nothing`() {
        assertEquals(KeyPlan.Action.Ignore, decide(openSearchOption = true, searchButtonFound = true, hasKey = false))
    }

    @Test fun `a field but no key is a reason to set the key up`() {
        assertEquals(KeyPlan.Action.NeedKey, decide(fieldFocused = true, hasKey = false))
    }
}
