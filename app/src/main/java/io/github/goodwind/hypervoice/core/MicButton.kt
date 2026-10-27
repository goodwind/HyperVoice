package io.github.goodwind.hypervoice.core

/**
 * Tells an app's own microphone button from the rest of its screen, by what accessibility says of it: the view id
 * and the description. Leanback, which most TV apps are built on, calls it `lb_search_bar_speech_orb` and labels it
 * only "Search Action", so the id is the dependable sign and the words are the fallback for other apps.
 */
object MicButton {
    private val splitter = Regex("[^a-z0-9]+")
    private val buttonish = setOf("button", "btn", "icon", "orb", "search")
    private val voiceWords = listOf("голосов", "микрофон", "voice", "microphone", "speak", "говор", "dictat")
    private val notTheButton = listOf("settings", "настрой", "mute", "off", "выкл", "отключ")

    fun matches(viewId: String?, description: CharSequence?): Boolean = byId(viewId) || byDescription(description)

    private fun byId(viewId: String?): Boolean {
        val name = viewId?.substringAfter("id/", "")?.lowercase().orEmpty()
        if (name.isEmpty()) return false
        val tokens = name.split(splitter).filter { it.isNotEmpty() }.toSet()
        val voice = "voice" in tokens && tokens.any { it in buttonish || it == "input" }
        val mic = ("mic" in tokens || "microphone" in tokens) && tokens.any { it in buttonish }
        return ("speech" in tokens && "orb" in tokens) || voice || mic
    }

    private fun byDescription(description: CharSequence?): Boolean {
        val text = description?.toString()?.lowercase().orEmpty()
        return text.isNotEmpty() && voiceWords.any { it in text } && notTheButton.none { it in text }
    }
}
