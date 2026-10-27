package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguagesTest {
    @Test fun `by default English and the language of the device`() {
        assertEquals(listOf("en", "ru"), Languages.defaults("ru"))
        assertEquals(listOf("en", "kk"), Languages.defaults("kk"))
    }

    @Test fun `English alone when the device speaks English or something not offered`() {
        assertEquals(listOf("en"), Languages.defaults("en"))
        assertEquals(listOf("en"), Languages.defaults("xx"))
        assertEquals(listOf("en"), Languages.defaults(""))
    }

    @Test fun `the hint names the chosen languages and gives a few commands in each`() {
        val h = Languages.hint(listOf("ru", "en"))
        assertTrue(h.contains("Russian and English"))
        assertTrue(h.contains("включи"))
        assertTrue(h.contains("play"))
        assertTrue(h.contains("YouTube"))
    }

    @Test fun `a hint for one language leaves the others out`() {
        val h = Languages.hint(listOf("en"))
        assertTrue(h.contains("English"))
        assertFalse(h.contains("Russian"))
        assertFalse(h.contains("включи"))
    }

    @Test fun `three languages are listed with commas and an and`() {
        assertTrue(Languages.hint(listOf("ru", "en", "kk")).contains("Russian, English and Kazakh"))
    }

    @Test fun `nothing chosen is the English hint, and unknown codes are skipped`() {
        assertEquals(Languages.hint(listOf("en")), Languages.hint(emptyList()))
        assertEquals(Languages.hint(listOf("en")), Languages.hint(listOf("zz", "en")))
    }

    @Test fun `the choice is kept as a comma separated list and read back without junk`() {
        assertEquals("ru,en", Languages.serialize(listOf("ru", "en")))
        assertEquals(listOf("ru", "en"), Languages.parse("ru, en,ru,zz"))
        assertEquals(emptyList<String>(), Languages.parse(""))
        assertEquals(emptyList<String>(), Languages.parse(null))
    }

    @Test fun `every offered language has a name, commands and a unique code`() {
        val codes = Languages.all.map { it.code }
        assertEquals(codes.size, codes.toSet().size)
        assertTrue(Languages.all.all { it.name.isNotBlank() && it.english.isNotBlank() && it.commands.isNotBlank() })
    }
}
