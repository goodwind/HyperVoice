package io.github.goodwind.hypervoice.core

import java.io.ByteArrayInputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HttpWireTest {
    private fun read(text: String, max: Int = 16 * 1024) = HttpWire.read(ByteArrayInputStream(text.toByteArray()), max)

    @Test fun `reads the method, the path and the body of a post`() {
        val r = read("POST /tok/key HTTP/1.1\r\nHost: tv\r\nContent-Length: 7\r\n\r\nkey=abc")!!
        assertEquals("POST", r.method)
        assertEquals("/tok/key", r.path)
        assertEquals("key=abc", r.body)
    }

    @Test fun `a get has no body`() {
        val r = read("GET /tok?x=1 HTTP/1.1\r\nHost: tv\r\n\r\n")!!
        assertEquals("GET", r.method)
        assertEquals("/tok?x=1", r.path)
        assertEquals("", r.body)
    }

    @Test fun `header names are not case sensitive`() {
        assertEquals("ab", read("POST /t HTTP/1.1\r\ncontent-length: 2\r\n\r\nab")!!.body)
    }

    @Test fun `the body is counted in bytes, not characters`() {
        assertEquals("ключ", read("POST /t HTTP/1.1\r\nContent-Length: 8\r\n\r\nключ")!!.body)
    }

    @Test fun `a body over the limit is refused`() {
        assertNull(read("POST /t HTTP/1.1\r\nContent-Length: 100\r\n\r\n" + "x".repeat(100), max = 50))
    }

    @Test fun `a body that stops short is refused`() {
        assertNull(read("POST /t HTTP/1.1\r\nContent-Length: 10\r\n\r\nabc"))
    }

    @Test fun `a request line that is not http is refused`() {
        assertNull(read("hello\r\n\r\n"))
        assertNull(read(""))
    }

    @Test fun `a bad length is refused`() {
        assertNull(read("POST /t HTTP/1.1\r\nContent-Length: -3\r\n\r\n"))
        assertNull(read("POST /t HTTP/1.1\r\nContent-Length: many\r\n\r\n"))
    }

    @Test fun `headers that never end are refused`() {
        assertNull(read("GET /t HTTP/1.1\r\n" + "X: y\r\n".repeat(5000), max = 1024))
    }

    @Test fun `a response says its length in bytes, closes and is not cached`() {
        val text = String(HttpWire.write(SetupWeb.Response(200, "text/plain; charset=utf-8", "ключ")))
        assertTrue(text.startsWith("HTTP/1.1 200 OK\r\n"))
        assertTrue(text.contains("Content-Length: 8\r\n"))
        assertTrue(text.contains("Connection: close\r\n"))
        assertTrue(text.contains("Cache-Control: no-store\r\n"))
        assertTrue(text.endsWith("\r\n\r\nключ"))
    }

    @Test fun `the reason phrases of the codes in use`() {
        assertTrue(String(HttpWire.write(SetupWeb.Response(404, "text/plain", ""))).startsWith("HTTP/1.1 404 Not Found"))
        assertTrue(String(HttpWire.write(SetupWeb.Response(405, "text/plain", ""))).startsWith("HTTP/1.1 405 Method Not Allowed"))
        assertTrue(String(HttpWire.write(SetupWeb.Response(400, "text/plain", ""))).startsWith("HTTP/1.1 400 Bad Request"))
    }
}
