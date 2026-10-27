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

    // The default languages follow where the TV is: the device's language, then what is spoken in its country
    // (the country of the locale, or of the time zone when the locale has none), at most two, and English.

    @Test fun `a locale and a country that agree`() {
        assertEquals(listOf("en", "ru"), Languages.defaults("ru", "RU"))
        assertEquals(listOf("en", "de"), Languages.defaults("de", "DE"))
        assertEquals(listOf("en", "de"), Languages.defaults("de", "AT"))
        assertEquals(listOf("en", "es"), Languages.defaults("es", "MX"))
    }

    @Test fun `the two languages most used in a country`() {
        assertEquals(listOf("en", "kk", "ru"), Languages.defaults("kk", "KZ"))
        assertEquals(listOf("en", "uk", "ru"), Languages.defaults("uk", "UA"))
        assertEquals(listOf("en", "be", "ru"), Languages.defaults("be", "BY"))
    }

    @Test fun `a Russian TV in Kazakhstan gets Kazakh too, by its time zone`() {
        assertEquals(listOf("en", "ru", "kk"), Languages.defaults("ru", "", "Asia/Almaty"))
        assertEquals(listOf("en", "ru", "kk"), Languages.defaults("ru", "RU", "Asia/Almaty"))
    }

    @Test fun `an English speaking country is English alone`() {
        assertEquals(listOf("en"), Languages.defaults("en", "US", "America/New_York"))
        assertEquals(listOf("en"), Languages.defaults("en", "GB"))
    }

    @Test fun `a country that is not known falls back to the language of the locale`() {
        assertEquals(listOf("en", "fr"), Languages.defaults("fr", "CA"))
        assertEquals(listOf("en", "tr"), Languages.defaults("tr", "ZZ"))
    }

    @Test fun `no more than two languages besides English`() {
        assertEquals(listOf("en", "ru", "kk"), Languages.defaults("ru", "KZ", "Asia/Almaty"))
        assertEquals(3, Languages.defaults("uk", "BY", "Europe/Kyiv").size)
    }

    @Test fun `a language that is not offered is dropped`() {
        assertEquals(listOf("en"), Languages.defaults("xx", "ZZ"))
        assertEquals(listOf("en", "ru"), Languages.defaults("xx", "RU"))
    }

    @Test fun `the languages to choose from include the common ones`() {
        val codes = Languages.all.map { it.code }
        for (c in listOf("en", "ru", "kk", "uk", "tr", "de", "fr", "es", "pt", "it", "pl", "ja", "ko", "zh", "ar", "hi", "uz", "az", "ka", "hy")) assertTrue(c, c in codes)
    }
}
