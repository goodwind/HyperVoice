package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** A microphone that plays a script: each entry is the number of samples the next read returns (0: nothing yet). */
private class ScriptedSource(private val script: List<Int>, private val failToStart: Boolean = false) : AudioSource {
    var started = false
    var stopped = false
    private var at = 0
    override fun start() {
        if (failToStart) throw IllegalStateException("no mic")
        started = true
    }
    override fun read(buffer: ShortArray): Int {
        val n = if (at < script.size) script[at++] else 0
        for (i in 0 until n) buffer[i] = 1000
        return n
    }
    override fun stop() { stopped = true }
}

private class RecordingListener : VoiceSession.Listener {
    val events = mutableListOf<String>()
    var result: Outcome? = null
    var onSpeechStartedHook: () -> Unit = {}
    var onLevelHook: (Int) -> Unit = {}
    private var levels = 0
    override fun onReady() { events += "ready" }
    override fun onSpeechStarted() { events += "started"; onSpeechStartedHook() }
    override fun onLevel(rms: Double) { events += "level"; onLevelHook(++levels) }
    override fun onSpeechEnded() { events += "ended" }
    override fun onResult(outcome: Outcome) { events += "result"; result = outcome }
}

private class FakeTranscriber(private val answer: Outcome = Outcome.Text("hello")) : Transcriber {
    val wavs = mutableListOf<ByteArray>()
    override fun transcribe(wav: ByteArray): Outcome { wavs += wav; return answer }
}

class VoiceSessionTest {
    private var clock = 0L
    private fun session(source: AudioSource, t: Transcriber, l: VoiceSession.Listener, startTimeoutMs: Long = 8000) =
        VoiceSession(source, t, l, now = { clock += 20; clock }, sleep = {}, sampleRate = 16000, startTimeoutMs = startTimeoutMs, silenceMs = 500, maxMs = 20000, minSpeechMs = 250)

    private val chunk = 1600 // 100 ms at 16 kHz

    @Test fun `speech then a gap is transcribed and the result reported`() {
        val src = ScriptedSource(listOf(0, 0, chunk, chunk, chunk, chunk))
        val tr = FakeTranscriber()
        val l = RecordingListener()
        session(src, tr, l).run()
        assertEquals(Outcome.Text("hello"), l.result)
        assertEquals(listOf("ready", "started", "ended", "result"), l.events.filter { it != "level" })
        assertTrue(src.started && src.stopped)
    }

    @Test fun `the clip sent is a wav of the speech with room round it`() {
        val src = ScriptedSource(listOf(chunk, chunk, chunk, chunk))
        val tr = FakeTranscriber()
        session(src, tr, RecordingListener()).run()
        val wav = tr.wavs.single()
        assertEquals("RIFF", String(wav, 0, 4))
        // 4 chunks of 100 ms, 300 ms of silence before and after: 1.0 s at 16 kHz, 16 bit
        assertEquals(44 + 32000, wav.size)
    }

    @Test fun `every chunk of audio reports its level`() {
        val src = ScriptedSource(listOf(chunk, chunk, chunk))
        val l = RecordingListener()
        session(src, FakeTranscriber(), l).run()
        assertEquals(3, l.events.count { it == "level" })
    }

    @Test fun `no audio in the start window is a timeout and nothing is sent`() {
        val src = ScriptedSource(emptyList())
        val tr = FakeTranscriber()
        val l = RecordingListener()
        session(src, tr, l, startTimeoutMs = 1000).run()
        assertEquals(Outcome.Timeout, l.result)
        assertEquals(0, tr.wavs.size)
        assertTrue(src.stopped)
    }

    @Test fun `a blip too short to be speech is not sent`() {
        val src = ScriptedSource(listOf(800)) // 50 ms
        val tr = FakeTranscriber()
        val l = RecordingListener()
        session(src, tr, l).run()
        assertEquals(Outcome.NoSpeech, l.result)
        assertEquals(0, tr.wavs.size)
    }

    @Test fun `finishing by hand ends the speech without waiting for a gap`() {
        val src = ScriptedSource(List(500) { chunk }) // audio never stops
        val tr = FakeTranscriber()
        val l = RecordingListener()
        val s = session(src, tr, l)
        // the button is let go after 300 ms of speech
        l.onLevelHook = { n -> if (n == 3) s.finish() }
        s.run()
        assertEquals(Outcome.Text("hello"), l.result)
        assertEquals(1, tr.wavs.size)
    }

    @Test fun `finishing by hand too soon is a blip and is not sent`() {
        val src = ScriptedSource(List(500) { chunk })
        val tr = FakeTranscriber()
        val l = RecordingListener()
        val s = session(src, tr, l)
        l.onLevelHook = { n -> if (n == 1) s.finish() }
        s.run()
        assertEquals(Outcome.NoSpeech, l.result)
        assertEquals(0, tr.wavs.size)
    }

    @Test fun `cancelling reports nothing and sends nothing`() {
        val src = ScriptedSource(List(50) { chunk })
        val tr = FakeTranscriber()
        val l = RecordingListener()
        val s = session(src, tr, l)
        l.onSpeechStartedHook = { s.cancel() }
        s.run()
        assertEquals(null, l.result)
        assertEquals(0, tr.wavs.size)
        assertTrue(src.stopped)
    }

    @Test fun `a microphone that will not start is an audio failure`() {
        val src = ScriptedSource(emptyList(), failToStart = true)
        val l = RecordingListener()
        session(src, FakeTranscriber(), l).run()
        assertEquals(Outcome.NoAudio, l.result)
        assertEquals(listOf("result"), l.events)
    }

    @Test fun `speech that never stops is cut at the limit and still transcribed`() {
        val src = ScriptedSource(List(2000) { chunk })
        val tr = FakeTranscriber()
        val l = RecordingListener()
        session(src, tr, l).run()
        assertEquals(1, tr.wavs.size)
        assertEquals(Outcome.Text("hello"), l.result)
    }

    @Test fun `a failure from the transcriber is passed on`() {
        val src = ScriptedSource(listOf(chunk, chunk, chunk))
        val l = RecordingListener()
        session(src, FakeTranscriber(Outcome.Network("down")), l).run()
        assertEquals(Outcome.Network("down"), l.result)
    }
}
