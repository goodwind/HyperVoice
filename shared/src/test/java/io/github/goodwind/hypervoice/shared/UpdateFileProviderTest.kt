package io.github.goodwind.hypervoice.shared

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class UpdateFileProviderTest {
    @get:Rule val tmp = TemporaryFolder()

    private fun dirWith(name: String): File = tmp.newFolder("update").also { File(it, name).writeText("apk") }

    @Test fun `a file in the directory is found`() {
        val dir = dirWith("a.apk")
        assertEquals(File(dir, "a.apk").canonicalFile, UpdateFileProvider.within(dir, "a.apk"))
    }

    @Test fun `a name that climbs out of the directory is refused`() {
        val dir = dirWith("a.apk")
        File(tmp.root, "secret").writeText("x")
        assertNull(UpdateFileProvider.within(dir, "../secret"))
    }

    @Test fun `a file in a subdirectory is refused`() {
        val dir = dirWith("a.apk")
        File(dir, "sub").mkdir(); File(dir, "sub/b.apk").writeText("x")
        assertNull(UpdateFileProvider.within(dir, "sub/b.apk"))
    }

    @Test fun `a missing file, a directory and no name are refused`() {
        val dir = dirWith("a.apk")
        File(dir, "sub").mkdir()
        assertNull(UpdateFileProvider.within(dir, "none.apk"))
        assertNull(UpdateFileProvider.within(dir, "sub"))
        assertNull(UpdateFileProvider.within(dir, null))
        assertNull(UpdateFileProvider.within(dir, ""))
    }
}
