package io.github.goodwind.hypervoice.core

import io.github.goodwind.hypervoice.shared.Reply
import io.github.goodwind.hypervoice.shared.Request
import io.github.goodwind.hypervoice.shared.Transport
import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Test

private class GetTransport(private val reply: Any) : Transport {
    val requests = mutableListOf<Request>()
    override fun post(request: Request): Reply = error("not used")
    override fun get(request: Request): Reply {
        requests += request
        if (reply is IOException) throw reply
        return reply as Reply
    }
}

class KeyCheckTest {
    @Test fun `the key is tried with a read that costs nothing`() {
        val t = GetTransport(Reply(200, "{}"))
        KeyCheck(t).check("secret")
        val r = t.requests.single()
        assertEquals("https://api.groq.com/openai/v1/models", r.url)
        assertEquals("Bearer secret", r.headers["Authorization"])
    }

    @Test fun `answers`() {
        assertEquals(KeyStatus.Valid, KeyCheck(GetTransport(Reply(200, "{}"))).check("k"))
        assertEquals(KeyStatus.Invalid, KeyCheck(GetTransport(Reply(401, "{}"))).check("k"))
        assertEquals(KeyStatus.Invalid, KeyCheck(GetTransport(Reply(403, "{}"))).check("k"))
        assertEquals(KeyStatus.Valid, KeyCheck(GetTransport(Reply(429, "{}"))).check("k")) // over its limit is still a key
        assertEquals(KeyStatus.Unreachable, KeyCheck(GetTransport(Reply(500, "{}"))).check("k"))
        assertEquals(KeyStatus.Unreachable, KeyCheck(GetTransport(IOException("down"))).check("k"))
    }

    @Test fun `no key is not sent anywhere`() {
        val t = GetTransport(Reply(200, "{}"))
        assertEquals(KeyStatus.Missing, KeyCheck(t).check("  "))
        assertEquals(0, t.requests.size)
    }
}
