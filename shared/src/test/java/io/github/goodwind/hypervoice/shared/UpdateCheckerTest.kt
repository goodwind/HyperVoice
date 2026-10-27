package io.github.goodwind.hypervoice.shared

import java.io.IOException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeGet(private val reply: Any) : Transport {
    val requests = mutableListOf<Request>()
    override fun post(request: Request): Reply = error("not used")
    override fun get(request: Request): Reply {
        requests += request
        if (reply is IOException) throw reply
        return reply as Reply
    }
}

class UpdateCheckerTest {
    private val body = """{"tag_name":"v0.2.0","draft":false,"prerelease":false,"html_url":"h","assets":[{"name":"HyperVoice-0.2.0.apk","browser_download_url":"u","size":1}]}"""
    private val apk = { n: String -> n.startsWith("HyperVoice-") }

    @Test fun `it asks github for the latest release of the repository`() {
        val t = FakeGet(Reply(200, body))
        UpdateChecker(t, "goodwind/HyperVoice").check("0.1.0", apk)
        val r = t.requests.single()
        assertEquals("https://api.github.com/repos/goodwind/HyperVoice/releases/latest", r.url)
        assertEquals("application/vnd.github+json", r.headers["Accept"])
        assertTrue(r.headers.getValue("User-Agent").isNotBlank())
    }

    @Test fun `an answer is turned into a result`() {
        assertTrue(UpdateChecker(FakeGet(Reply(200, body)), "a/b").check("0.1.0", apk) is UpdateResult.Available)
        assertEquals(UpdateResult.UpToDate, UpdateChecker(FakeGet(Reply(200, body)), "a/b").check("0.2.0", apk))
    }

    @Test fun `no release yet is not a failure`() {
        assertEquals(UpdateResult.NoRelease, UpdateChecker(FakeGet(Reply(404, "{}")), "a/b").check("0.1.0", apk))
    }

    @Test fun `a refusal or a lost connection is a failure that says so`() {
        assertTrue(UpdateChecker(FakeGet(Reply(403, "{}")), "a/b").check("0.1.0", apk) is UpdateResult.Failed)
        assertTrue(UpdateChecker(FakeGet(IOException("down")), "a/b").check("0.1.0", apk) is UpdateResult.Failed)
    }
}
