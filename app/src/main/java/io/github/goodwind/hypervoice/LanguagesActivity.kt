package io.github.goodwind.hypervoice

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import io.github.goodwind.hypervoice.core.Languages

/** The languages Whisper is told to expect: OK on a row toggles it; the first row goes back to the defaults. */
class LanguagesActivity : Activity() {
    private lateinit var settings: Settings
    private val rows = mutableMapOf<String, FocusRow>()

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        settings = Settings(this)
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(28), dp(48), dp(28))
        }
        column.addView(label(getString(R.string.languages_title), 28f, bold = true))
        column.addView(label(getString(R.string.languages_hint), 14f, Ink.SECONDARY).apply { setPadding(0, dp(4), 0, dp(14)) })
        val list = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = plate(Ink.PANEL, 16)
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }
        list.addView(FocusRow(this, getString(R.string.languages_defaults), null) {
            settings.languages = settings.defaultLanguages(); refresh()
        })
        for (l in Languages.all) {
            val row = FocusRow(this, "${l.name}  (${l.english})", null, slider = true) { toggle(l.code) }
            rows[l.code] = row
            list.addView(row)
        }
        column.addView(list, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Ink.BACKGROUND)
            isFillViewport = true
            isFocusable = false
            addView(column)
        })
        refresh()
        list.getChildAt(0).requestFocus()
    }

    private fun toggle(code: String) {
        val now = settings.languages
        settings.languages = if (code in now) now - code else now + code
        refresh()
    }

    private fun refresh() {
        val chosen = settings.languages
        for ((code, row) in rows) row.setOn(code in chosen)
    }
}
