package io.github.goodwind.hypervoice.core

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/** Just enough HTTP/1.1 for the setup page: one request in, one answer out, then the connection closes. */
object HttpWire {
    class Request(val method: String, val path: String, val body: String)

    /** The request on [input], or null if it is not one we serve: not HTTP, too big ([max] bytes of head or body), or cut short. */
    fun read(input: InputStream, max: Int): Request? {
        val head = readHead(input, max) ?: return null
        val lines = head.split("\r\n")
        val parts = lines[0].split(' ')
        if (parts.size != 3 || !parts[2].startsWith("HTTP/") || parts[0].isEmpty() || !parts[1].startsWith("/")) return null
        var length = 0
        for (line in lines.drop(1)) {
            val at = line.indexOf(':')
            if (at > 0 && line.substring(0, at).trim().equals("Content-Length", ignoreCase = true)) {
                length = line.substring(at + 1).trim().toIntOrNull() ?: return null
            }
        }
        if (length < 0 || length > max) return null
        val body = ByteArray(length)
        var got = 0
        while (got < length) {
            val n = try { input.read(body, got, length - got) } catch (e: IOException) { return null }
            if (n < 0) return null
            got += n
        }
        return Request(parts[0], parts[1], String(body, Charsets.UTF_8))
    }

    fun write(r: SetupWeb.Response): ByteArray {
        val body = r.body.toByteArray(Charsets.UTF_8)
        val head = "HTTP/1.1 ${r.code} ${reason(r.code)}\r\n" +
            "Content-Type: ${r.contentType}\r\n" +
            "Content-Length: ${body.size}\r\n" +
            "Cache-Control: no-store\r\n" +
            "Connection: close\r\n\r\n"
        return head.toByteArray(Charsets.UTF_8) + body
    }

    private fun readHead(input: InputStream, max: Int): String? {
        val out = ByteArrayOutputStream()
        var tail = 0
        while (out.size() <= max) {
            val b = try { input.read() } catch (e: IOException) { return null }
            if (b < 0) return null
            out.write(b)
            // \r\n\r\n ends the head
            tail = when {
                b == '\r'.code && (tail == 0 || tail == 2) -> tail + 1
                b == '\n'.code && (tail == 1 || tail == 3) -> tail + 1
                b == '\r'.code -> 1
                else -> 0
            }
            if (tail == 4) return String(out.toByteArray(), 0, out.size() - 4, Charsets.UTF_8)
        }
        return null
    }

    private fun reason(code: Int) = when (code) {
        200 -> "OK"
        400 -> "Bad Request"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        else -> "Error"
    }
}
