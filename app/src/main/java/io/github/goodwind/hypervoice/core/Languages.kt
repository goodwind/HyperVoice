package io.github.goodwind.hypervoice.core

/**
 * The languages a person can ask Whisper to expect. Whisper is told them in a prompt (its `language` setting takes
 * only one, and a phrase like "включи Imagine Dragons" has two), so each language brings a few commands to prime it.
 */
object Languages {
    class Language(val code: String, val name: String, val english: String, val commands: String)

    val all = listOf(
        Language("en", "English", "English", "play, search, open, volume up"),
        Language("ru", "Русский", "Russian", "включи, найди, открой, громче"),
        Language("kk", "Қазақша", "Kazakh", "қос, іздеу, ашу, дыбысты көтер"),
        Language("uk", "Українська", "Ukrainian", "увімкни, знайди, відкрий, гучніше"),
        Language("be", "Беларуская", "Belarusian", "уключы, знайдзі, адкрый, гучней"),
        Language("tr", "Türkçe", "Turkish", "oynat, ara, aç, sesi aç"),
        Language("de", "Deutsch", "German", "spiele, suche, öffne, lauter"),
        Language("fr", "Français", "French", "joue, cherche, ouvre, plus fort"),
        Language("es", "Español", "Spanish", "reproduce, busca, abre, más alto"),
        Language("pl", "Polski", "Polish", "włącz, szukaj, otwórz, głośniej"),
        Language("zh", "中文", "Chinese", "播放, 搜索, 打开, 大声点"),
    )

    private val byCode = all.associateBy { it.code }
    private const val BRANDS = "YouTube, Netflix, iPhone, Wi-Fi."

    /** English, and the language the device is set to if it is one of the offered. */
    fun defaults(deviceLanguage: String): List<String> =
        listOf("en", deviceLanguage.lowercase()).filter { it in byCode }.distinct()

    fun hint(codes: List<String>): String {
        val chosen = codes.mapNotNull { byCode[it] }.distinct().ifEmpty { listOf(byCode.getValue("en")) }
        val names = chosen.map { it.english }
        val list = if (names.size == 1) names[0] else names.dropLast(1).joinToString(", ") + " and " + names.last()
        return "Voice commands in $list: " + chosen.joinToString(", ") { it.commands } + ". " + BRANDS
    }

    fun serialize(codes: List<String>): String = codes.joinToString(",")

    fun parse(stored: String?): List<String> =
        stored.orEmpty().split(',').map { it.trim() }.filter { it in byCode }.distinct()
}
