package io.github.goodwind.hypervoice.core

/**
 * Works out where a phrase begins and ends from when audio arrives. The remote's microphone sends audio only
 * while its button is held, so a gap after audio is the end; no audio at all in the start window is a timeout.
 * Clock-free: the caller says what time it is.
 */
class Endpointing(private val startTimeoutMs: Long, private val silenceMs: Long, private val maxMs: Long) {
    enum class Event { SpeechStarted, SpeechEnded, Timeout, TooLong }

    private var beganAt = 0L
    private var speechAt = 0L
    private var lastAudioAt = 0L
    private var started = false
    private var done = false

    fun begin(nowMs: Long) {
        beganAt = nowMs
        started = false
        done = false
    }

    fun onAudio(nowMs: Long): Event? {
        if (done) return null
        lastAudioAt = nowMs
        if (started) return null
        started = true
        speechAt = nowMs
        return Event.SpeechStarted
    }

    fun tick(nowMs: Long): Event? {
        if (done) return null
        val event = when {
            !started -> if (nowMs - beganAt >= startTimeoutMs) Event.Timeout else null
            nowMs - speechAt >= maxMs -> Event.TooLong
            nowMs - lastAudioAt >= silenceMs -> Event.SpeechEnded
            else -> null
        }
        if (event != null) done = true
        return event
    }

    /** The caller knows the phrase is over (the button was let go). */
    fun finish(): Event? {
        if (done) return null
        done = true
        return if (started) Event.SpeechEnded else Event.Timeout
    }
}
