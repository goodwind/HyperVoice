package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AccessibilityListTest {
    private val ours = "io.github.goodwind.hypervoice/.HyperVoiceAccessibilityService"

    @Test fun `adds ours at the end and keeps the others`() {
        assertEquals("a/.A:b/.B:$ours", AccessibilityList.with("a/.A:b/.B", ours))
    }

    @Test fun `an empty or missing list becomes just ours`() {
        assertEquals(ours, AccessibilityList.with("", ours))
        assertEquals(ours, AccessibilityList.with(null, ours))
        assertEquals(ours, AccessibilityList.with("null", ours))
    }

    @Test fun `ours already there is left alone`() {
        assertEquals("a/.A:$ours", AccessibilityList.with("a/.A:$ours", ours))
    }

    @Test fun `empty entries are dropped`() {
        assertEquals("a/.A:$ours", AccessibilityList.with("a/.A::", ours))
    }

    @Test fun `knows whether the package is in the list`() {
        assertEquals(true, AccessibilityList.has("a/.A:$ours", "io.github.goodwind.hypervoice"))
        assertEquals(false, AccessibilityList.has("a/.A", "io.github.goodwind.hypervoice"))
        assertEquals(false, AccessibilityList.has(null, "io.github.goodwind.hypervoice"))
        assertEquals(false, AccessibilityList.has("io.github.goodwind.hypervoice.other/.X", "io.github.goodwind.hypervoice"))
    }
}
