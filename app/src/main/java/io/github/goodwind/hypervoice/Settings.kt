package io.github.goodwind.hypervoice

import android.content.Context
import io.github.goodwind.hypervoice.core.HttpTransport
import io.github.goodwind.hypervoice.core.Languages
import io.github.goodwind.hypervoice.core.Transcriber
import io.github.goodwind.hypervoice.core.WhisperClient
import java.util.Locale

/** What the person has chosen: the Groq key and the languages Whisper is primed with. */
class Settings(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("hypervoice", Context.MODE_PRIVATE)

    /** The key set in the app; failing that the one the build was made with (the proof of concept's). */
    var key: String
        get() = prefs.getString(KEY, null)?.trim().orEmpty().ifEmpty { BuildConfig.GROQ_KEY }
        set(value) = prefs.edit().putString(KEY, value.trim()).apply()

    val hasKey: Boolean get() = key.isNotBlank()

    /** English and the language of the device until the person says otherwise. */
    var languages: List<String>
        get() = Languages.parse(prefs.getString(LANGUAGES, null)).ifEmpty { Languages.defaults(Locale.getDefault().language) }
        set(value) = prefs.edit().putString(LANGUAGES, Languages.serialize(value)).apply()

    fun transcriber(): Transcriber = WhisperClient(HttpTransport(), key, MODEL, Languages.hint(languages))

    companion object {
        const val MODEL = "whisper-large-v3-turbo"
        private const val KEY = "groq_key"
        private const val LANGUAGES = "languages"
    }
}
