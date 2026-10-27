package io.github.goodwind.hypervoice.core

import io.github.goodwind.hypervoice.shared.Reply
import io.github.goodwind.hypervoice.shared.Request
import io.github.goodwind.hypervoice.shared.Transport
import java.io.IOException
import org.json.JSONException
import org.json.JSONObject

/**
 * Speech to text through Groq's hosted Whisper. A refused key and a spent limit are final; an empty
 * answer (a very short clip sometimes gets one), a server error and a dropped connection are tried once more.
 */
class WhisperClient(
    private val transport: Transport,
    private val key: String,
    private val model: String,
    private val hint: String,
    private val endpoint: String = ENDPOINT,
) : Transcriber {
    override fun transcribe(wav: ByteArray): Outcome {
        if (key.isBlank()) return Outcome.NoKey
        var last: Outcome = Outcome.NoSpeech
        repeat(2) {
            when (val outcome = once(wav)) {
                is Outcome.Text, Outcome.Unauthorized, Outcome.RateLimited -> return outcome
                else -> last = outcome
            }
        }
        return last
    }

    private fun once(wav: ByteArray): Outcome {
        val form = Multipart().apply {
            field("model", model)
            field("response_format", "json")
            field("temperature", "0")
            field("prompt", hint)
            file("file", "speech.wav", "audio/wav", wav)
        }
        val request = Request(endpoint, mapOf("Authorization" to "Bearer $key", "Content-Type" to form.contentType), form.build())
        val reply = try {
            transport.post(request)
        } catch (e: IOException) {
            return Outcome.Network(e.message ?: "io")
        }
        return when (reply.code) {
            200 -> parse(reply)
            401, 403 -> Outcome.Unauthorized
            429 -> Outcome.RateLimited
            else -> Outcome.Server(reply.code)
        }
    }

    private fun parse(reply: Reply): Outcome = try {
        val text = JSONObject(reply.body).optString("text").trim()
        if (text.isEmpty()) Outcome.NoSpeech else Outcome.Text(text)
    } catch (e: JSONException) {
        Outcome.Server(reply.code)
    }

    companion object {
        const val ENDPOINT = "https://api.groq.com/openai/v1/audio/transcriptions"
    }
}
