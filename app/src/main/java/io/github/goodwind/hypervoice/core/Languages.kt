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
        Language("pt", "Português", "Portuguese", "toca, procura, abre, aumenta o volume"),
        Language("it", "Italiano", "Italian", "riproduci, cerca, apri, alza il volume"),
        Language("nl", "Nederlands", "Dutch", "speel af, zoek, open, harder"),
        Language("ja", "日本語", "Japanese", "再生, 検索, 開いて, 音量を上げて"),
        Language("ko", "한국어", "Korean", "재생, 검색, 열어줘, 볼륨 올려"),
        Language("ar", "العربية", "Arabic", "شغّل, ابحث, افتح, ارفع الصوت"),
        Language("hi", "हिन्दी", "Hindi", "चलाओ, खोजो, खोलो, आवाज़ बढ़ाओ"),
        Language("id", "Bahasa Indonesia", "Indonesian", "putar, cari, buka, naikkan volume"),
        Language("vi", "Tiếng Việt", "Vietnamese", "phát, tìm, mở, tăng âm lượng"),
        Language("uz", "Oʻzbekcha", "Uzbek", "qoʻy, qidir, och, ovozni oshir"),
        Language("az", "Azərbaycanca", "Azerbaijani", "oxut, axtar, aç, səsi artır"),
        Language("ka", "ქართული", "Georgian", "ჩართე, მოძებნე, გახსენი, ხმა აუწიე"),
        Language("hy", "Հայերեն", "Armenian", "միացրու, փնտրիր, բացիր, ձայնը բարձրացրու"),
    )

    private val byCode = all.associateBy { it.code }
    private const val BRANDS = "YouTube, Netflix, iPhone, Wi-Fi."

    /** The languages most spoken in a country, the most used first, as far as the offered ones go. */
    private val byCountry: Map<String, List<String>> = buildMap {
        put("KZ", listOf("ru", "kk")); put("RU", listOf("ru")); put("UA", listOf("uk", "ru")); put("BY", listOf("be", "ru"))
        put("UZ", listOf("uz", "ru")); put("AZ", listOf("az", "ru")); put("GE", listOf("ka", "ru")); put("AM", listOf("hy", "ru"))
        put("KG", listOf("ru")); put("TJ", listOf("ru")); put("MD", listOf("ru"))
        put("TR", listOf("tr")); put("DE", listOf("de")); put("AT", listOf("de")); put("CH", listOf("de", "fr"))
        put("FR", listOf("fr")); put("BE", listOf("nl", "fr")); put("NL", listOf("nl")); put("ES", listOf("es"))
        put("PT", listOf("pt")); put("BR", listOf("pt")); put("IT", listOf("it")); put("PL", listOf("pl"))
        put("JP", listOf("ja")); put("KR", listOf("ko")); put("CN", listOf("zh")); put("TW", listOf("zh")); put("HK", listOf("zh"))
        put("IN", listOf("hi")); put("ID", listOf("id")); put("VN", listOf("vi"))
        for (c in listOf("MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU", "BO", "DO", "HN", "PY", "SV", "NI", "CR", "PA", "UY")) put(c, listOf("es"))
        for (c in listOf("SA", "AE", "EG", "IQ", "JO", "KW", "LB", "MA", "DZ", "TN", "LY", "QA", "OM", "BH", "SY", "YE", "SD")) put(c, listOf("ar"))
    }

    /** Where a time zone is, for a TV whose locale names no country (or the wrong one). */
    private val zoneCountry: Map<String, String> = buildMap {
        for (z in listOf("Asia/Almaty", "Asia/Qyzylorda", "Asia/Qostanay", "Asia/Aqtobe", "Asia/Aqtau", "Asia/Atyrau", "Asia/Oral")) put(z, "KZ")
        for (z in listOf("Europe/Moscow", "Europe/Kaliningrad", "Europe/Samara", "Europe/Volgograd", "Asia/Yekaterinburg", "Asia/Omsk", "Asia/Novosibirsk", "Asia/Krasnoyarsk", "Asia/Irkutsk", "Asia/Yakutsk", "Asia/Vladivostok", "Asia/Magadan", "Asia/Kamchatka")) put(z, "RU")
        put("Europe/Kyiv", "UA"); put("Europe/Kiev", "UA"); put("Europe/Minsk", "BY"); put("Asia/Tashkent", "UZ"); put("Asia/Samarkand", "UZ")
        put("Asia/Bishkek", "KG"); put("Asia/Dushanbe", "TJ"); put("Asia/Yerevan", "AM"); put("Asia/Baku", "AZ"); put("Asia/Tbilisi", "GE")
        put("Europe/Istanbul", "TR"); put("Europe/Berlin", "DE"); put("Europe/Paris", "FR"); put("Europe/Madrid", "ES"); put("Europe/Rome", "IT")
        put("Europe/Warsaw", "PL"); put("Europe/Amsterdam", "NL"); put("Europe/Lisbon", "PT"); put("Asia/Tokyo", "JP"); put("Asia/Seoul", "KR")
        put("Asia/Shanghai", "CN"); put("Asia/Taipei", "TW"); put("Asia/Kolkata", "IN"); put("Asia/Calcutta", "IN"); put("Asia/Jakarta", "ID"); put("Asia/Ho_Chi_Minh", "VN")
    }

    /**
     * What to expect when nothing is chosen: English, and the language the TV is set to, then what is most spoken
     * where it is (by the locale's country, and by its time zone), at most two languages besides English.
     * A country that is not known leaves the language of the locale.
     */
    fun defaults(language: String, country: String = "", timeZone: String = ""): List<String> {
        val here = byCountry[country.uppercase()].orEmpty() + byCountry[zoneCountry[timeZone]].orEmpty()
        val spoken = (listOf(language.lowercase()) + here).filter { it in byCode && it != "en" }.distinct().take(2)
        return listOf("en") + spoken
    }

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
