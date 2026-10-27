package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchButtonTest {
    @Test fun `ids that name a search button`() {
        for (id in listOf("x:id/search_button", "x:id/btn_search", "x:id/search_orb", "x:id/search_icon", "x:id/action_search")) {
            assertTrue(id, SearchButton.matches(id, null))
        }
    }

    @Test fun `ids of a search box, results or the microphone are not it`() {
        for (id in listOf("x:id/lb_search_text_editor", "x:id/lb_search_frame", "x:id/search_results", "x:id/search_input", "x:id/lb_search_bar_speech_orb", "x:id/search_bar_edit_text", "x:id/research_button")) {
            assertFalse(id, SearchButton.matches(id, null))
        }
    }

    @Test fun `descriptions that say search, and only that`() {
        for (d in listOf("Search", "search", "Search Action", "Search button", "Поиск", "Найти", "Искать")) assertTrue(d, SearchButton.matches(null, d))
        for (d in listOf("Search results", "Recent searches", "Voice search", "Голосовой поиск", "Settings", "", "Search in channel")) assertFalse(d, SearchButton.matches(null, d))
    }

    @Test fun `nothing to go on`() {
        assertFalse(SearchButton.matches(null, null))
    }
}
