package io.github.goodwind.hypervoice

import io.github.goodwind.hypervoice.core.HttpWire
import io.github.goodwind.hypervoice.core.SetupWeb
import java.io.IOException
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors

/**
 * The page a phone opens from the QR code. It runs only while the settings screen is open: [start] on the way in,
 * [stop] on the way out. The routing and the rules are [SetupWeb]; this is the sockets.
 */
class SetupServer(private val web: SetupWeb, private val onChange: () -> Unit) {
    private var server: ServerSocket? = null
    private val pool = Executors.newFixedThreadPool(4)

    /** The port it listens on, or 0 if it does not. */
    @Synchronized fun start(): Int {
        server?.let { return it.localPort }
        val s = try { ServerSocket(0) } catch (e: IOException) { return 0 }
        server = s
        Thread({ accept(s) }, "hypervoice-setup").apply { isDaemon = true }.start()
        return s.localPort
    }

    @Synchronized fun stop() {
        try { server?.close() } catch (_: IOException) { }
        server = null
    }

    private fun accept(s: ServerSocket) {
        while (!s.isClosed) {
            val c = try { s.accept() } catch (e: IOException) { return }
            try { pool.execute { serve(c) } } catch (e: java.util.concurrent.RejectedExecutionException) { c.close() }
        }
    }

    private fun serve(c: Socket) = c.use {
        try {
            c.soTimeout = 5_000
            val request = HttpWire.read(c.getInputStream(), MAX_BYTES)
            val answer = if (request == null) SetupWeb.Response(400, "text/plain; charset=utf-8", "Bad request")
            else web.handle(request.method, request.path, request.body)
            c.getOutputStream().apply { write(HttpWire.write(answer)); flush() }
            if (request != null && request.method == "POST") onChange()
        } catch (_: IOException) { }
    }

    private companion object { const val MAX_BYTES = 16 * 1024 }
}
