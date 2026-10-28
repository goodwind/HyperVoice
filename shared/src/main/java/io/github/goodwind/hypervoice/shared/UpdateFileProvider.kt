package io.github.goodwind.hypervoice.shared

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Lends the installer the one update file in cache/update, and nothing else: it is another app, and may read
 * only what a URI handed to it names. Not exported; the access comes with the intent's read grant.
 */
class UpdateFileProvider : ContentProvider() {
    override fun onCreate() = true

    private fun file(uri: Uri): File =
        within(File(context!!.cacheDir, DIR), uri.lastPathSegment) ?: throw FileNotFoundException(uri.toString())

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("read only")
        return ParcelFileDescriptor.open(file(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?): Cursor {
        val file = file(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        return MatrixCursor(columns).apply {
            addRow(columns.map { if (it == OpenableColumns.SIZE) file.length() else file.name })
        }
    }

    override fun getType(uri: Uri) = "application/vnd.android.package-archive"
    override fun insert(uri: Uri, values: ContentValues?): Uri? = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0

    companion object {
        private const val DIR = "update"

        /** The file called [name] directly in [dir], or null: no such file, or a name that leads out of [dir]. */
        fun within(dir: File, name: String?): File? {
            if (name.isNullOrEmpty()) return null
            val home = dir.canonicalFile
            val file = File(home, name).canonicalFile
            return file.takeIf { it.parentFile == home && it.isFile }
        }

        /** The URI of [apk], which must sit in cache/update. */
        fun uriFor(context: Context, apk: File): Uri =
            Uri.Builder().scheme("content").authority("${context.packageName}.updates").appendPath(apk.name).build()
    }
}
