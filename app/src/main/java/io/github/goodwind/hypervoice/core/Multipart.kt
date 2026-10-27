package io.github.goodwind.hypervoice.core

import java.io.ByteArrayOutputStream
import java.util.UUID

/** A multipart/form-data body, built by hand: the platform has none and a library would be the app's biggest part. */
class Multipart(private val boundary: String = "----hypervoice" + UUID.randomUUID().toString().replace("-", "")) {
    private val out = ByteArrayOutputStream()

    val contentType: String get() = "multipart/form-data; boundary=$boundary"

    fun field(name: String, value: String) {
        out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(value.toByteArray(Charsets.UTF_8))
        out.write("\r\n".toByteArray())
    }

    fun file(name: String, filename: String, contentType: String, bytes: ByteArray) {
        out.write("--$boundary\r\nContent-Disposition: form-data; name=\"$name\"; filename=\"$filename\"\r\nContent-Type: $contentType\r\n\r\n".toByteArray(Charsets.UTF_8))
        out.write(bytes)
        out.write("\r\n".toByteArray())
    }

    fun build(): ByteArray = out.toByteArray() + "--$boundary--\r\n".toByteArray()
}
