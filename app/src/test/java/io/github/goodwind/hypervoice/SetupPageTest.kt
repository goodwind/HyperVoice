package io.github.goodwind.hypervoice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetupPageTest {
    private val page = SetupPage.html("tok123")

    @Test fun `posts the key under the token it was given`() {
        assertTrue(page.contains("x.open('POST', '/tok123/key')"))
        assertFalse(page.contains("\${token}"))
    }

    @Test fun `speaks the twenty languages of the app`() {
        for (code in listOf("en", "ru", "uk", "kk", "zhCN", "zhTW", "es", "pt", "fr", "de", "it", "ja", "ko", "ar", "hi", "tr", "pl", "id", "vi", "nl")) {
            assertTrue(code, Regex("\"$code\"\\s*:\\s*\\[").containsMatchIn(page))
        }
    }

    @Test fun `every language gives the eight sentences the script reads`() {
        val body = page.substringAfter("var TEXTS = ").substringBefore(";\nvar tag")
        val rows = Regex("\"(\\w+)\"\\s*:\\s*\\[(.*?)\\]").findAll(body).toList()
        assertEquals(20, rows.size)
        for (row in rows) {
            val sentences = Regex("\"((?:[^\"\\\\]|\\\\.)*)\"").findAll(row.groupValues[2]).count()
            assertEquals(row.groupValues[1], 8, sentences)
        }
    }

    @Test fun `the lead leaves room for the link in every language`() {
        val body = page.substringAfter("var TEXTS = ").substringBefore(";\nvar tag")
        assertEquals(20, Regex("\\{link\\}").findAll(body).count())
    }
}
