package io.github.goodwind.hypervoice.core

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

data class Request(val url: String, val headers: Map<String, String>, val body: ByteArray)

data class Reply(val code: Int, val body: String)

/** How a request gets to the server. Faked in the tests; [HttpTransport] is the real one. */
interface Transport {
    @Throws(IOException::class)
    fun post(request: Request): Reply
}

class HttpTransport(private val connectMs: Int = 8_000, private val readMs: Int = 15_000) : Transport {
    override fun post(request: Request): Reply {
        val c = URL(request.url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = connectMs
            c.readTimeout = readMs
            c.doOutput = true
            c.requestMethod = "POST"
            request.headers.forEach { (k, v) -> c.setRequestProperty(k, v) }
            c.outputStream.use { it.write(request.body) }
            val code = c.responseCode
            val stream = if (code < 400) c.inputStream else c.errorStream
            return Reply(code, stream?.use { String(it.readBytes(), Charsets.UTF_8) }.orEmpty())
        } finally {
            c.disconnect()
        }
    }
}
