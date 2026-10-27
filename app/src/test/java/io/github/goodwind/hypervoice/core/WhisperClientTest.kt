package io.github.goodwind.hypervoice.core

import io.github.goodwind.hypervoice.shared.Reply
import io.github.goodwind.hypervoice.shared.Request
import io.github.goodwind.hypervoice.shared.Transport
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeTransport(private val replies: MutableList<Any>) : Transport {
    val requests = mutableListOf<Request>()
    override fun get(request: Request): Reply = error("not used")
    override fun post(request: Request): Reply {
        requests += request
        val next = replies.removeAt(0)
        if (next is IOException) throw next
        return next as Reply
    }
}

class WhisperClientTest {
    private val wav = byteArrayOf(1, 2, 3)
    private fun ok(text: String) = Reply(200, """{"text":"$text"}""")
    private fun client(t: Transport, key: String = "k") = WhisperClient(t, key, model = "whisper-large-v3-turbo", hint = "hint text")

    @Test fun `a transcript comes back trimmed`() {
        val t = FakeTransport(mutableListOf(ok("  Hello world.  ")))
        assertEquals(Outcome.Text("Hello world."), client(t).transcribe(wav))
    }

    @Test fun `the request goes to groq with the key, the model, the hint and the clip`() {
        val t = FakeTransport(mutableListOf(ok("x")))
        client(t, key = "secret").transcribe(wav)
        val r = t.requests.single()
        assertEquals("https://api.groq.com/openai/v1/audio/transcriptions", r.url)
        assertEquals("Bearer secret", r.headers["Authorization"])
        assertTrue(r.headers.getValue("Content-Type").startsWith("multipart/form-data; boundary="))
        val body = String(r.body, Charsets.UTF_8)
        assertTrue(body.contains("name=\"model\"\r\n\r\nwhisper-large-v3-turbo"))
        assertTrue(body.contains("name=\"prompt\"\r\n\r\nhint text"))
        assertTrue(body.contains("name=\"response_format\"\r\n\r\njson"))
        assertTrue(body.contains("name=\"temperature\"\r\n\r\n0"))
        assertTrue(body.contains("name=\"file\"; filename=\"speech.wav\""))
    }

    @Test fun `an empty answer is asked for again once`() {
        val t = FakeTransport(mutableListOf(ok(""), ok("Найди рецепт борща.")))
        assertEquals(Outcome.Text("Найди рецепт борща."), client(t).transcribe(wav))
        assertEquals(2, t.requests.size)
    }

    @Test fun `two empty answers are no speech`() {
        val t = FakeTransport(mutableListOf(ok(""), ok("   ")))
        assertEquals(Outcome.NoSpeech, client(t).transcribe(wav))
        assertEquals(2, t.requests.size)
    }

    @Test fun `a refused key is not retried`() {
        val t = FakeTransport(mutableListOf(Reply(401, """{"error":{"message":"Invalid API Key"}}""")))
        assertEquals(Outcome.Unauthorized, client(t).transcribe(wav))
        assertEquals(1, t.requests.size)
    }

    @Test fun `a forbidden key is unauthorized too`() {
        val t = FakeTransport(mutableListOf(Reply(403, "{}")))
        assertEquals(Outcome.Unauthorized, client(t).transcribe(wav))
    }

    @Test fun `too many requests is rate limited and not retried`() {
        val t = FakeTransport(mutableListOf(Reply(429, "{}")))
        assertEquals(Outcome.RateLimited, client(t).transcribe(wav))
        assertEquals(1, t.requests.size)
    }

    @Test fun `a server error is tried once more`() {
        val t = FakeTransport(mutableListOf(Reply(503, "{}"), ok("Привет")))
        assertEquals(Outcome.Text("Привет"), client(t).transcribe(wav))
        assertEquals(2, t.requests.size)
    }

    @Test fun `two server errors are a server failure with the code`() {
        val t = FakeTransport(mutableListOf(Reply(500, "{}"), Reply(502, "{}")))
        assertEquals(Outcome.Server(502), client(t).transcribe(wav))
    }

    @Test fun `a network failure is tried once more and then reported`() {
        val t = FakeTransport(mutableListOf(IOException("down"), IOException("still down")))
        val out = client(t).transcribe(wav)
        assertTrue(out is Outcome.Network)
        assertEquals(2, t.requests.size)
    }

    @Test fun `a network failure that clears is a transcript`() {
        val t = FakeTransport(mutableListOf(IOException("blip"), ok("Hi")))
        assertEquals(Outcome.Text("Hi"), client(t).transcribe(wav))
    }

    @Test fun `without a key nothing is sent`() {
        val t = FakeTransport(mutableListOf())
        assertEquals(Outcome.NoKey, client(t, key = "").transcribe(wav))
        assertEquals(0, t.requests.size)
    }

    @Test fun `an answer that is not json is a server failure not a crash`() {
        val t = FakeTransport(mutableListOf(Reply(200, "<html>"), Reply(200, "<html>")))
        assertEquals(Outcome.Server(200), client(t).transcribe(wav))
    }
}
