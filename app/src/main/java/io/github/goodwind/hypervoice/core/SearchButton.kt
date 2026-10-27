package io.github.goodwind.hypervoice.core

/**
 * Tells an app's search button (the one that opens its search, where a field is then in focus) from a search box,
 * its results and the microphone, by the view id and the description accessibility gives it.
 */
object SearchButton {
    private val splitter = Regex("[^a-z0-9]+")
    private val buttonish = setOf("button", "btn", "icon", "orb", "action")
    private val notIt = setOf("speech", "voice", "mic", "microphone", "editor", "edit", "text", "input", "frame", "results", "result", "bar")
    private val words = setOf("search", "search action", "search button", "поиск", "найти", "искать")

    fun matches(viewId: String?, description: CharSequence?): Boolean = byId(viewId) || words.contains(description?.toString()?.trim()?.lowercase().orEmpty())

    private fun byId(viewId: String?): Boolean {
        val name = viewId?.substringAfter("id/", "")?.lowercase().orEmpty()
        if (name.isEmpty()) return false
        val tokens = name.split(splitter).filter { it.isNotEmpty() }.toSet()
        return "search" in tokens && tokens.any { it in buttonish } && tokens.none { it in notIt }
    }
}
