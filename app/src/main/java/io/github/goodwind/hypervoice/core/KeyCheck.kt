package io.github.goodwind.hypervoice.core

import io.github.goodwind.hypervoice.shared.Request
import io.github.goodwind.hypervoice.shared.Transport
import java.io.IOException

enum class KeyStatus { Valid, Invalid, Unreachable, Missing }

/** Tries a Groq key with a request that costs nothing: the list of models. */
class KeyCheck(private val transport: Transport) {
    fun check(key: String): KeyStatus {
        if (key.isBlank()) return KeyStatus.Missing
        val reply = try {
            transport.get(Request("https://api.groq.com/openai/v1/models", mapOf("Authorization" to "Bearer ${key.trim()}")))
        } catch (e: IOException) {
            return KeyStatus.Unreachable
        }
        return when (reply.code) {
            200, 429 -> KeyStatus.Valid // over its limit is still a key
            401, 403 -> KeyStatus.Invalid
            else -> KeyStatus.Unreachable
        }
    }
}
