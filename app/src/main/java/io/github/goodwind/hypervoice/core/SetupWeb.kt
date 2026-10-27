package io.github.goodwind.hypervoice.core

import java.net.URLDecoder
import org.json.JSONArray
import org.json.JSONObject

/** The fields of an application/x-www-form-urlencoded body. */
object Form {
    fun parse(body: String): Map<String, String> {
        if (body.isEmpty()) return emptyMap()
        return body.split('&').filter { it.isNotEmpty() }.associate {
            val at = it.indexOf('=')
            if (at < 0) URLDecoder.decode(it, "UTF-8") to "" else URLDecoder.decode(it.substring(0, at), "UTF-8") to URLDecoder.decode(it.substring(at + 1), "UTF-8")
        }
    }
}

/**
 * The page a phone opens (from the QR code on the TV) to give the app its key and choose the languages. This is
 * the routing and the rules, with no sockets in it: [handle] takes a request and gives the answer. Everything is under
 * the one-off token in the path; anything else is not found, and does not say why.
 */
class SetupWeb(
    private val token: String,
    private val store: Store,
    private val keyCheck: (String) -> KeyStatus,
    private val defaults: () -> List<String>,
    private val page: (String) -> String,
) {
    interface Store {
        var key: String
        val hasKey: Boolean
        var languages: List<String>
        var pressMicButton: Boolean
        var openSearchButton: Boolean
    }

    class Response(val code: Int, val contentType: String, val body: String)

    fun handle(method: String, rawPath: String, body: String): Response {
        val path = rawPath.substringBefore('?')
        val prefix = "/$token"
        if (path != prefix && !path.startsWith("$prefix/")) return notFound()
        return when (path.removePrefix(prefix)) {
            "", "/" -> if (method == "GET") Response(200, "text/html; charset=utf-8", page(token)) else notAllowed()
            "/state" -> if (method == "GET") json(200, state()) else notAllowed()
            "/key" -> if (method == "POST") setKey(Form.parse(body)["key"].orEmpty().trim()) else notAllowed()
            "/languages" -> if (method == "POST") setLanguages(Form.parse(body)["codes"].orEmpty()) else notAllowed()
            "/options" -> if (method == "POST") setOptions(Form.parse(body)) else notAllowed()
            else -> notFound()
        }
    }

    private fun state() = JSONObject()
        .put("hasKey", store.hasKey)
        .put("pressMic", store.pressMicButton)
        .put("openSearch", store.openSearchButton)
        .put("languages", JSONArray(store.languages))
        .put("all", JSONArray().also { all -> Languages.all.forEach { all.put(JSONObject().put("code", it.code).put("name", it.name).put("english", it.english)) } })

    private fun setKey(key: String): Response {
        if (key.isEmpty()) return json(400, JSONObject().put("ok", false).put("error", "empty"))
        if (key.length > MAX_KEY) return json(400, JSONObject().put("ok", false).put("error", "too long"))
        return when (keyCheck(key)) {
            KeyStatus.Invalid -> json(200, JSONObject().put("ok", false).put("error", "invalid"))
            KeyStatus.Unreachable -> { store.key = key; json(200, JSONObject().put("ok", true).put("warning", "unverified")) }
            else -> { store.key = key; json(200, JSONObject().put("ok", true)) }
        }
    }

    private fun setLanguages(codes: String): Response {
        val chosen = Languages.parse(codes).ifEmpty { defaults() }
        store.languages = chosen
        return json(200, JSONObject().put("ok", true).put("languages", JSONArray(chosen)))
    }

    private fun setOptions(form: Map<String, String>): Response {
        form["pressMic"]?.let { store.pressMicButton = it == "true" }
        form["openSearch"]?.let { store.openSearchButton = it == "true" }
        return json(200, JSONObject().put("ok", true))
    }

    private fun json(code: Int, o: JSONObject) = Response(code, "application/json; charset=utf-8", o.toString())
    private fun notFound() = Response(404, "text/plain; charset=utf-8", "Not found")
    private fun notAllowed() = Response(405, "text/plain; charset=utf-8", "Method not allowed")

    private companion object { const val MAX_KEY = 4096 }
}
