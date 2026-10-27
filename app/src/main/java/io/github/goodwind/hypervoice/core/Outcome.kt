package io.github.goodwind.hypervoice.core

/** What came of turning a clip into text. */
sealed interface Outcome {
    data class Text(val text: String) : Outcome
    data object NoSpeech : Outcome
    data object Timeout : Outcome
    data object NoAudio : Outcome
    data object NoKey : Outcome
    data object Unauthorized : Outcome
    data object RateLimited : Outcome
    data class Server(val code: Int) : Outcome
    data class Network(val reason: String) : Outcome

    /** The android.speech.SpeechRecognizer error a client is told, or null for a transcript. */
    fun recognizerError(): Int? = when (this) {
        is Text -> null
        NoSpeech -> 7 // ERROR_NO_MATCH
        Timeout -> 6 // ERROR_SPEECH_TIMEOUT
        NoAudio -> 3 // ERROR_AUDIO
        NoKey, Unauthorized -> 9 // ERROR_INSUFFICIENT_PERMISSIONS
        RateLimited, is Server -> 4 // ERROR_SERVER
        is Network -> 2 // ERROR_NETWORK
    }

    /** One short sentence for the person at the TV. */
    fun message(): String = when (this) {
        is Text -> text
        NoSpeech -> "Не расслышал"
        Timeout -> "Не услышал"
        NoAudio -> "Нет доступа к микрофону"
        NoKey -> "Нет ключа Groq"
        Unauthorized -> "Ключ Groq не принят"
        RateLimited -> "Лимит Groq исчерпан"
        is Server -> "Ошибка Groq $code"
        is Network -> "Нет связи с Groq"
    }
}
