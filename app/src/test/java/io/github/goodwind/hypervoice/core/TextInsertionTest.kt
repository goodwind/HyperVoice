package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Test

class TextInsertionTest {
    @Test fun `into an empty field`() {
        val r = TextInsertion.insert(null, 0, 0, "hello")
        assertEquals("hello", r.text)
        assertEquals(5, r.cursor)
    }

    @Test fun `after existing text a space is put between`() {
        val r = TextInsertion.insert("foo", 3, 3, "hello")
        assertEquals("foo hello", r.text)
        assertEquals(9, r.cursor)
    }

    @Test fun `no second space after a space`() {
        assertEquals("foo hello", TextInsertion.insert("foo ", 4, 4, "hello").text)
    }

    @Test fun `in the middle with spaces kept around`() {
        val r = TextInsertion.insert("abcd", 2, 2, "hello")
        assertEquals("ab hello cd", r.text)
        assertEquals(8, r.cursor)
    }

    @Test fun `a selection is replaced`() {
        val r = TextInsertion.insert("foo bar baz", 4, 7, "hello")
        assertEquals("foo hello baz", r.text)
        assertEquals(9, r.cursor)
    }

    @Test fun `a cursor that is not known goes to the end`() {
        assertEquals("foo hello", TextInsertion.insert("foo", -1, -1, "hello").text)
    }

    @Test fun `a selection the wrong way round is read the right way`() {
        assertEquals("foo hello baz", TextInsertion.insert("foo bar baz", 7, 4, "hello").text)
    }

    @Test fun `a cursor past the end is held to it`() {
        assertEquals("foo hello", TextInsertion.insert("foo", 99, 99, "hello").text)
    }

    @Test fun `one sentence loses its full stop for a search box`() {
        assertEquals("Найди рецепт борща", Dictation.clean("Найди рецепт борща."))
        assertEquals("what is the weather tomorrow?", Dictation.clean(" what is the weather tomorrow? "))
    }

    @Test fun `more than one sentence keeps its punctuation`() {
        assertEquals("Привет. Как дела?", Dictation.clean("Привет. Как дела?"))
    }

    @Test fun `an abbreviation or number with a dot inside is left alone`() {
        assertEquals("Wi-Fi 5.8", Dictation.clean("Wi-Fi 5.8"))
    }
}
