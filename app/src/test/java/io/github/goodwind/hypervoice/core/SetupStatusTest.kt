package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupStatusTest {
    private val good = SetupStatus.Snapshot(audioPermission = true, accessibilityOn = true, recognizerIsOurs = true, hasKey = true, remoteMicFound = true, canRepair = false)

    @Test fun `all in place is ready`() {
        assertTrue(SetupStatus.ready(good))
        assertTrue(SetupStatus.checks(good).all { it.ok })
    }

    @Test fun `the checks come in the order a person would fix them`() {
        assertEquals(
            listOf(SetupStatus.Item.Permission, SetupStatus.Item.Accessibility, SetupStatus.Item.Recognizer, SetupStatus.Item.Key, SetupStatus.Item.RemoteMic),
            SetupStatus.checks(good).map { it.item },
        )
    }

    @Test fun `each thing that is missing makes it not ready`() {
        assertFalse(SetupStatus.ready(good.copy(audioPermission = false)))
        assertFalse(SetupStatus.ready(good.copy(accessibilityOn = false)))
        assertFalse(SetupStatus.ready(good.copy(recognizerIsOurs = false)))
        assertFalse(SetupStatus.ready(good.copy(hasKey = false)))
    }

    @Test fun `the remote not being found is listed but does not block`() {
        val s = good.copy(remoteMicFound = false)
        assertTrue(SetupStatus.ready(s))
        assertFalse(SetupStatus.checks(s).first { it.item == SetupStatus.Item.RemoteMic }.ok)
    }

    @Test fun `what the app can put right itself, only if it may`() {
        val broken = good.copy(accessibilityOn = false, recognizerIsOurs = false, hasKey = false, audioPermission = false)
        assertEquals(emptyList<SetupStatus.Item>(), SetupStatus.repairable(broken))
        assertEquals(listOf(SetupStatus.Item.Accessibility, SetupStatus.Item.Recognizer), SetupStatus.repairable(broken.copy(canRepair = true)))
        assertEquals(emptyList<SetupStatus.Item>(), SetupStatus.repairable(good.copy(canRepair = true)))
    }
}
