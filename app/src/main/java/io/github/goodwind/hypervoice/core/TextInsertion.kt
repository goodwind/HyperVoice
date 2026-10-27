package io.github.goodwind.hypervoice.core

/** Putting dictated text into a field that already holds some. */
object TextInsertion {
    data class Result(val text: String, val cursor: Int)

    /** [selStart]..[selEnd] is the cursor or selection (negative: unknown, so the end). Spaces are kept between words. */
    fun insert(existing: CharSequence?, selStart: Int, selEnd: Int, add: String): Result {
        val base = existing?.toString().orEmpty()
        var a = if (selStart < 0 || selEnd < 0) base.length else selStart
        var b = if (selStart < 0 || selEnd < 0) base.length else selEnd
        a = a.coerceIn(0, base.length)
        b = b.coerceIn(0, base.length)
        if (a > b) a = b.also { b = a }
        val before = base.substring(0, a)
        val after = base.substring(b)
        val lead = if (before.isNotEmpty() && !before.last().isWhitespace()) " " else ""
        val trail = if (after.isNotEmpty() && !after.first().isWhitespace()) " " else ""
        return Result(before + lead + add + trail + after, before.length + lead.length + add.length)
    }
}

/** How a transcript is tidied for a field. */
object Dictation {
    private val boundary = Regex("[.!?]\\s")

    /** Whisper ends a phrase with a full stop; in a search box that is noise. A longer text keeps its punctuation. */
    fun clean(text: String): String {
        val t = text.trim()
        if (t.endsWith("..") || !t.endsWith(".") || boundary.containsMatchIn(t)) return t
        return t.dropLast(1)
    }
}
