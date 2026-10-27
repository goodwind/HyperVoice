package io.github.goodwind.hypervoice

import android.content.Context
import io.github.goodwind.hypervoice.core.Languages
import io.github.goodwind.hypervoice.core.SetupWeb
import io.github.goodwind.hypervoice.core.Transcriber
import io.github.goodwind.hypervoice.core.WhisperClient
import io.github.goodwind.hypervoice.shared.HttpTransport
import java.util.Locale

/** What the person has chosen: the Groq key and the languages Whisper is primed with. */
class Settings(context: Context) : SetupWeb.Store {
    private val prefs = context.applicationContext.getSharedPreferences("hypervoice", Context.MODE_PRIVATE)

    /** The key set in the app; failing that the one the build was made with (the proof of concept's). */
    override var key: String
        get() = prefs.getString(KEY, null)?.trim().orEmpty().ifEmpty { BuildConfig.GROQ_KEY }
        set(value) = prefs.edit().putString(KEY, value.trim()).apply()

    override val hasKey: Boolean get() = key.isNotBlank()

    /** English and the language of the device until the person says otherwise. */
    override var languages: List<String>
        get() = Languages.parse(prefs.getString(LANGUAGES, null)).ifEmpty { Languages.defaults(Locale.getDefault().language) }
        set(value) = prefs.edit().putString(LANGUAGES, Languages.serialize(value)).apply()

    /** With no field to type into, press an app's own microphone button if the screen has one. On: it is what people like. */
    override var pressMicButton: Boolean
        get() = prefs.getBoolean(PRESS_MIC, true)
        set(value) = prefs.edit().putBoolean(PRESS_MIC, value).apply()

    /** With no field and no microphone button, press a search button, and dictate into the field that takes focus. Off: it moves the screen. */
    override var openSearchButton: Boolean
        get() = prefs.getBoolean(OPEN_SEARCH, false)
        set(value) = prefs.edit().putBoolean(OPEN_SEARCH, value).apply()

    fun transcriber(): Transcriber = WhisperClient(HttpTransport(), key, MODEL, Languages.hint(languages))

    companion object {
        const val MODEL = "whisper-large-v3-turbo"
        private const val KEY = "groq_key"
        private const val LANGUAGES = "languages"
        private const val PRESS_MIC = "press_mic_button"
        private const val OPEN_SEARCH = "open_search_button"
    }
}
