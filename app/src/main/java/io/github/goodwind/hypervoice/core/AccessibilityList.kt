package io.github.goodwind.hypervoice.core

/** The colon-separated list in the secure setting `enabled_accessibility_services`. */
object AccessibilityList {
    /** [list] with [service] added at the end if it is not in it; the others are kept as they are. */
    fun with(list: String?, service: String): String {
        val entries = entries(list)
        return (if (service in entries) entries else entries + service).joinToString(":")
    }

    /** Whether a service of [packageName] is in [list]. */
    fun has(list: String?, packageName: String) = entries(list).any { it.startsWith("$packageName/") }

    private fun entries(list: String?) = list.orEmpty().split(':').filter { it.isNotEmpty() && it != "null" }
}
