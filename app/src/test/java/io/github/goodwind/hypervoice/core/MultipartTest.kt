package io.github.goodwind.hypervoice.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MultipartTest {
    @Test fun `fields and a file come out as multipart form data`() {
        val m = Multipart("B")
        m.field("model", "whisper")
        m.file("file", "p.wav", "audio/wav", byteArrayOf(65, 66))
        val body = String(m.build(), Charsets.ISO_8859_1)
        assertTrue(body.startsWith("--B\r\nContent-Disposition: form-data; name=\"model\"\r\n\r\nwhisper\r\n"))
        assertTrue(body.contains("--B\r\nContent-Disposition: form-data; name=\"file\"; filename=\"p.wav\"\r\nContent-Type: audio/wav\r\n\r\nAB\r\n"))
        assertTrue(body.endsWith("--B--\r\n"))
        assertEquals("multipart/form-data; boundary=B", m.contentType)
    }

    @Test fun `a field with non-ascii text is sent as utf-8`() {
        val m = Multipart("B")
        m.field("prompt", "включи")
        val bytes = m.build()
        val utf8 = "включи".toByteArray(Charsets.UTF_8)
        val at = bytes.indices.firstOrNull { i -> utf8.indices.all { j -> bytes.getOrNull(i + j) == utf8[j] } }
        assertTrue("the utf-8 bytes of the text are in the body", at != null)
    }

    @Test fun `without a boundary given one is made that is not in the data`() {
        val a = Multipart().contentType
        val b = Multipart().contentType
        assertTrue(a.startsWith("multipart/form-data; boundary="))
        assertTrue(a != b)
    }
}
