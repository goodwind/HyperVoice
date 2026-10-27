package io.github.goodwind.hypervoice.core

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeStore : SetupWeb.Store {
    override var key: String = ""
    override val hasKey get() = key.isNotBlank()
    override var languages: List<String> = listOf("en", "ru")
    override var pressMicButton: Boolean = false
    override var openSearchButton: Boolean = false
}

class SetupWebTest {
    private val store = FakeStore()
    private var keyStatus = KeyStatus.Valid
    private val web = SetupWeb("tok123", store, keyCheck = { keyStatus }, defaults = { listOf("en", "ru", "kk") }, page = { "<html>$it</html>" })

    private fun json(r: SetupWeb.Response) = JSONObject(r.body)

    @Test fun `the page is served under the token`() {
        val r = web.handle("GET", "/tok123/", "")
        assertEquals(200, r.code)
        assertTrue(r.contentType.startsWith("text/html"))
        assertEquals("<html>tok123</html>", r.body)
        assertEquals(200, web.handle("GET", "/tok123", "").code)
    }

    @Test fun `anything else is not found, and says nothing about the token`() {
        for (path in listOf("/", "/other/", "/tok12/", "/tok1234/", "/tok123x", "/state", "/tok123/nope")) {
            val r = web.handle("GET", path, "")
            assertEquals(path, 404, r.code)
            assertFalse(r.body.contains("tok123"))
        }
        assertEquals(404, web.handle("POST", "/other/key", "key=a").code)
    }

    @Test fun `the state says what is set and what can be chosen, never the key`() {
        store.key = "gsk_secret"
        store.pressMicButton = true
        val r = web.handle("GET", "/tok123/state", "")
        val j = json(r)
        assertEquals(200, r.code)
        assertTrue(j.getBoolean("hasKey"))
        assertTrue(j.getBoolean("pressMic"))
        assertEquals("en,ru", (0 until j.getJSONArray("languages").length()).joinToString(",") { j.getJSONArray("languages").getString(it) })
        val all = j.getJSONArray("all")
        assertTrue((0 until all.length()).any { all.getJSONObject(it).getString("code") == "ru" })
        assertFalse(r.body.contains("gsk_secret"))
    }

    @Test fun `a good key is kept`() {
        val r = web.handle("POST", "/tok123/key", "key=gsk_abc")
        assertEquals(200, r.code)
        assertTrue(json(r).getBoolean("ok"))
        assertEquals("gsk_abc", store.key)
        assertFalse(r.body.contains("gsk_abc"))
    }

    @Test fun `a key the service refuses is not kept`() {
        keyStatus = KeyStatus.Invalid
        val r = web.handle("POST", "/tok123/key", "key=gsk_bad")
        assertFalse(json(r).getBoolean("ok"))
        assertEquals("invalid", json(r).getString("error"))
        assertEquals("", store.key)
    }

    @Test fun `a key that could not be tried is kept, with a warning`() {
        keyStatus = KeyStatus.Unreachable
        val r = web.handle("POST", "/tok123/key", "key=gsk_abc")
        assertTrue(json(r).getBoolean("ok"))
        assertEquals("unverified", json(r).getString("warning"))
        assertEquals("gsk_abc", store.key)
    }

    @Test fun `a key is read out of a form, spaces and escapes included, and trimmed`() {
        web.handle("POST", "/tok123/key", "key=%20gsk_a%2Bb%3D+")
        assertEquals("gsk_a+b=", store.key)
    }

    @Test fun `an empty or huge key is a bad request`() {
        assertEquals(400, web.handle("POST", "/tok123/key", "key=").code)
        assertEquals(400, web.handle("POST", "/tok123/key", "other=1").code)
        assertEquals(400, web.handle("POST", "/tok123/key", "key=" + "a".repeat(5000)).code)
        assertEquals("", store.key)
    }

    @Test fun `languages are kept, unknown ones dropped`() {
        val r = web.handle("POST", "/tok123/languages", "codes=ru%2Cen%2Czz")
        assertTrue(json(r).getBoolean("ok"))
        assertEquals(listOf("ru", "en"), store.languages)
    }

    @Test fun `no language chosen puts the defaults back`() {
        web.handle("POST", "/tok123/languages", "codes=")
        assertEquals(listOf("en", "ru", "kk"), store.languages)
    }

    @Test fun `the option to press an app's microphone button`() {
        web.handle("POST", "/tok123/options", "pressMic=true")
        assertTrue(store.pressMicButton)
        web.handle("POST", "/tok123/options", "pressMic=false")
        assertFalse(store.pressMicButton)
    }

    @Test fun `the option to open an app's search and dictate`() {
        web.handle("POST", "/tok123/options", "openSearch=true")
        assertTrue(store.openSearchButton)
        assertTrue(JSONObject(web.handle("GET", "/tok123/state", "").body).getBoolean("openSearch"))
        web.handle("POST", "/tok123/options", "openSearch=false")
        assertFalse(store.openSearchButton)
    }

    @Test fun `a method that makes no sense is refused`() {
        assertEquals(405, web.handle("PUT", "/tok123/key", "key=a").code)
        assertEquals(405, web.handle("GET", "/tok123/key", "").code)
        assertEquals(405, web.handle("POST", "/tok123/state", "").code)
    }
}

class FormTest {
    @Test fun `fields are split, decoded and kept by name`() {
        val f = Form.parse("a=1&b=hello+world&c=%D0%BF%D1%80&d")
        assertEquals("1", f["a"])
        assertEquals("hello world", f["b"])
        assertEquals("пр", f["c"])
        assertEquals("", f["d"])
    }

    @Test fun `nothing is nothing`() {
        assertTrue(Form.parse("").isEmpty())
    }
}
