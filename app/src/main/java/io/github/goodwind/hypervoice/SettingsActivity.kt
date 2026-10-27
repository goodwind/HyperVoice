package io.github.goodwind.hypervoice

import android.app.Activity
import android.os.Bundle
import android.provider.Settings as SystemSettings
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import io.github.goodwind.hypervoice.core.Languages

/** The key and the languages. Plain widgets that the remote's arrow keys reach. */
class SettingsActivity : Activity() {
    private lateinit var settings: Settings
    private lateinit var status: TextView

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        settings = Settings(this)
        intent.getStringExtra(EXTRA_KEY)?.let { settings.key = it } // for setting it up from a computer with adb
        val needsKey = intent.getBooleanExtra(EXTRA_NO_KEY, false) && !settings.hasKey

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(80, 50, 80, 30)
            setBackgroundColor(0xFF111114.toInt())
        }
        root.addView(title(getString(if (needsKey) R.string.set_key else R.string.app_name), if (needsKey) 0xFFFFB74D.toInt() else 0xFFFFFFFF.toInt()))
        status = title("", 0xFFB0B0B8.toInt()).apply { textSize = 20f }
        root.addView(status)
        root.addView(title(getString(R.string.settings_languages_header), 0xFFFFFFFF.toInt()).apply { textSize = 22f; setPadding(0, 28, 0, 8) })

        val list = ListView(this).apply {
            choiceMode = ListView.CHOICE_MODE_MULTIPLE
            adapter = ArrayAdapter(this@SettingsActivity, android.R.layout.simple_list_item_multiple_choice, Languages.all.map { "${it.name}  (${it.english})" })
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)
        }
        val chosen = settings.languages
        Languages.all.forEachIndexed { i, l -> list.setItemChecked(i, l.code in chosen) }
        list.setOnItemClickListener { _, _, _, _ ->
            settings.languages = Languages.all.filterIndexed { i, _ -> list.isItemChecked(i) }.map { it.code }
            refresh()
        }
        root.addView(list)
        setContentView(root)
        refresh()
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refresh()
    }

    private fun title(text: String, color: Int) = TextView(this).apply {
        this.text = text
        textSize = 30f
        setTextColor(color)
        gravity = Gravity.START
    }

    private fun refresh() {
        val enabled = SystemSettings.Secure.getString(contentResolver, SystemSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            .orEmpty().contains("$packageName/")
        val recognizer = SystemSettings.Secure.getString(contentResolver, "voice_recognition_service").orEmpty().startsWith(packageName)
        status.text = listOf(
            getString(if (settings.hasKey) R.string.settings_key_set else R.string.settings_key_missing),
            getString(if (enabled) R.string.settings_a11y_on else R.string.settings_a11y_off),
            getString(if (recognizer) R.string.settings_recognizer_ours else R.string.settings_recognizer_other),
            getString(R.string.settings_languages, settings.languages.joinToString(", ")),
        ).joinToString("\n")
    }

    companion object {
        const val EXTRA_NO_KEY = "no_key"
        const val EXTRA_KEY = "groq_key"
    }
}
