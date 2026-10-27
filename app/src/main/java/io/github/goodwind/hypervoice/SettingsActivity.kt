package io.github.goodwind.hypervoice

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import io.github.goodwind.hypervoice.core.KeyCheck
import io.github.goodwind.hypervoice.core.Languages
import io.github.goodwind.hypervoice.core.SetupStatus
import io.github.goodwind.hypervoice.core.SetupWeb
import io.github.goodwind.hypervoice.shared.ApkInstaller
import io.github.goodwind.hypervoice.shared.Downloads
import io.github.goodwind.hypervoice.shared.HttpTransport
import io.github.goodwind.hypervoice.shared.Update
import io.github.goodwind.hypervoice.shared.UpdateChecker
import io.github.goodwind.hypervoice.shared.UpdateResult
import java.io.File
import java.security.SecureRandom

/**
 * A header with the state of the setup (and a button to put right what the app can), the Whisper key's QR code
 * while there is none, and below it the options and a link to the languages. Plain widgets the remote's arrows reach.
 */
class SettingsActivity : Activity() {
    private lateinit var settings: Settings
    private lateinit var header: LinearLayout
    private lateinit var pressMic: FocusRow
    private lateinit var openSearch: FocusRow
    private lateinit var languages: FocusRow
    private var updateLine: String? = null
    private var updateReady = false
    private var offered: Update? = null
    private var updating = false
    private var checked = false
    private var server: SetupServer? = null
    private var url: String? = null
    private var failed = false

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        settings = Settings(this)
        // For setting it up from a computer with adb: the key, and the two options.
        intent.getStringExtra(EXTRA_KEY)?.let { settings.key = it }
        if (intent.hasExtra(EXTRA_PRESS_MIC)) settings.pressMicButton = intent.getBooleanExtra(EXTRA_PRESS_MIC, true)
        if (intent.hasExtra(EXTRA_OPEN_SEARCH)) settings.openSearchButton = intent.getBooleanExtra(EXTRA_OPEN_SEARCH, false)

        header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            background = plate(Ink.PANEL, 16)
            setPadding(dp(24), dp(20), dp(24), dp(20))
        }
        pressMic = FocusRow(this, getString(R.string.option_press_mic), null, slider = true) {
            settings.pressMicButton = !settings.pressMicButton; refreshRows()
        }
        openSearch = FocusRow(this, getString(R.string.option_open_search), null, slider = true) {
            settings.openSearchButton = !settings.openSearchButton; refreshRows()
        }
        languages = FocusRow(this, getString(R.string.settings_languages), null) {
            startActivity(Intent(this, LanguagesActivity::class.java))
        }
        val rows = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, 0)
            addView(pressMic)
            addView(openSearch)
            addView(languages)
        }
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(48), dp(20), dp(48), dp(14))
            addView(header, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
            addView(rows, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f))
            addView(footer())
        }
        setContentView(ScrollView(this).apply {
            setBackgroundColor(Ink.BACKGROUND)
            isFillViewport = true
            isFocusable = false
            addView(column)
        })
    }

    override fun onResume() {
        super.onResume()
        startServer()
        checkForUpdate()
        refresh()
        refreshRows()
    }

    /** The first control takes focus once the window is up; before that there is nothing for the remote to move from. */
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && currentFocus !is FocusRow && currentFocus !is FocusButton) (header.findViewWithTag<View>(REPAIR) ?: pressMic).requestFocus()
    }

    /** Asks GitHub once per visit to this screen; nothing is shown unless there is something to install. */
    private fun checkForUpdate() {
        if (checked) return
        checked = true
        val installed = try { packageManager.getPackageInfo(packageName, 0).versionName.orEmpty() } catch (e: Exception) { return }
        Thread {
            val result = UpdateChecker(HttpTransport(), REPO).check(installed) { it.startsWith("HyperVoice-") }
            if (result is UpdateResult.Available) runOnUiThread { offered = result.update; showUpdate(getString(R.string.update_available, result.update.version), getString(R.string.update_install)) }
        }.start()
    }

    /** What the header says about an update: [state] is the action it offers (null while it is being got). */
    private fun showUpdate(name: String, state: String?) {
        updateLine = if (state != null) "$name · $state" else name
        updateReady = state != null
        refresh()
        header.post {
            if (currentFocus !is FocusRow && currentFocus !is FocusButton) (header.findViewWithTag<View>(UPDATE) ?: header.findViewWithTag<View>(REPAIR) ?: pressMic).requestFocus()
        }
    }

    private fun installUpdate() {
        val u = offered ?: return
        if (updating) return
        updating = true
        val file = File(cacheDir, "update/${u.apkName}").also { it.parentFile?.mkdirs() }
        Thread {
            try {
                Downloads.fetch(u.apkUrl, file, u.sha256) { done, total ->
                    if (total > 0) runOnUiThread { showUpdate(getString(R.string.update_downloading, (done * 100 / total).toInt()), null) }
                }
                runOnUiThread { showUpdate(getString(R.string.update_installing), null) }
                ApkInstaller.install(this, file) { problem ->
                    updating = false
                    if (problem != null) runOnUiThread { showUpdate(getString(R.string.update_failed, problem), getString(R.string.update_install)) }
                }
            } catch (e: Exception) {
                updating = false
                runOnUiThread { showUpdate(getString(R.string.update_failed, e.message.orEmpty()), getString(R.string.update_install)) }
            }
        }.start()
    }

    override fun onPause() {
        server?.stop()
        server = null
        super.onPause()
    }

    private fun startServer() {
        url = null
        val ip = Network.lanAddress() ?: return
        val token = ByteArray(12).also { SecureRandom().nextBytes(it) }
            .joinToString("") { "%02x".format(it) }
        val web = SetupWeb(
            token = token,
            store = settings,
            keyCheck = { KeyCheck(HttpTransport()).check(it) },
            defaults = { settings.defaultLanguages() },
            page = { SetupPage.html(it) },
        )
        val s = SetupServer(web) { runOnUiThread { refresh(); refreshRows() } }
        val port = s.start()
        if (port == 0) return
        server = s
        url = "http://$ip:$port/$token/"
    }

    /** Who made it, under what licence, and where the source is. */
    private fun footer() = label(
        getString(R.string.footer_line, try { packageManager.getPackageInfo(packageName, 0).versionName } catch (e: Exception) { "" }),
        13f, Ink.SECONDARY,
    ).apply {
        gravity = Gravity.CENTER
        setPadding(0, dp(12), 0, 0)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    private fun refreshRows() {
        pressMic.setOn(settings.pressMicButton)
        openSearch.setOn(settings.openSearchButton)
        languages.setState(Languages.all.filter { it.code in settings.languages }.joinToString(", ") { it.name })
    }

    private fun refresh() {
        val snapshot = Repair.snapshot(this, settings)
        val ready = SetupStatus.ready(snapshot)
        val fixable = SetupStatus.repairable(snapshot)
        header.removeAllViews()

        val left = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        left.addView(
            LinearLayout(this).apply {
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(this@SettingsActivity).apply { setImageResource(R.drawable.ic_launcher) }, LinearLayout.LayoutParams(dp(48), dp(48)).apply { marginEnd = dp(14) })
                addView(label(getString(R.string.app_name), 28f, bold = true))
            },
        )
        left.addView(
            when {
                failed -> label(getString(R.string.status_repair_failed), 18f, Ink.RED, bold = true)
                ready -> label(getString(R.string.status_ready), 18f, Ink.GREEN, bold = true)
                else -> label(getString(R.string.status_needs_attention), 18f, Ink.ORANGE, bold = true)
            }.apply { setPadding(0, dp(4), 0, dp(10)) },
        )
        for (check in SetupStatus.checks(snapshot)) {
            left.addView(label("${if (check.ok) "✓" else "✕"}  ${getString(nameOf(check.item))}", 15f, if (check.ok) Ink.SECONDARY else Ink.LABEL))
        }
        if (fixable.isNotEmpty()) {
            left.addView(FocusButton(this, getString(R.string.action_repair)) {
                failed = !Repair.apply(this, fixable)
                refresh()
                (header.findViewWithTag<View>(REPAIR) ?: pressMic).requestFocus()
            }.apply { tag = REPAIR }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) })
        } else if (!ready && !snapshot.canRepair && (!snapshot.accessibilityOn || !snapshot.recognizerIsOurs)) {
            left.addView(label(getString(R.string.hint_no_repair), 13f, Ink.SECONDARY).apply { setPadding(0, dp(12), 0, 0) })
        }
        updateLine?.let { line ->
            val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(14) }
            if (updateReady) left.addView(FocusButton(this, line) { installUpdate() }.apply { tag = UPDATE }, params)
            else left.addView(label(line, 16f, Ink.ACCENT, bold = true), params)
        }
        header.addView(left, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))

        header.addView(keyPanel())
    }

    private fun keyPanel(): View {
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
        }
        val link = url
        if (link == null) {
            box.addView(label(getString(R.string.key_no_network), 14f, Ink.SECONDARY).apply { maxWidth = dp(240) })
            return box
        }
        val side = dp(190)
        box.addView(
            ImageView(this).apply {
                setImageBitmap(Qr.bitmap(link, side))
                setBackgroundColor(Color.WHITE)
            },
            LinearLayout.LayoutParams(side, side),
        )
        box.addView(
            label(getString(if (settings.hasKey) R.string.key_scan_caption_change else R.string.key_scan_caption), 14f, Ink.SECONDARY).apply {
                gravity = Gravity.CENTER
                maxWidth = side
                setPadding(0, dp(8), 0, 0)
            },
        )
        return box
    }

    private fun nameOf(item: SetupStatus.Item) = when (item) {
        SetupStatus.Item.Permission -> R.string.check_permission
        SetupStatus.Item.Accessibility -> R.string.check_accessibility
        SetupStatus.Item.Recognizer -> R.string.check_recognizer
        SetupStatus.Item.Key -> R.string.check_key
        SetupStatus.Item.RemoteMic -> R.string.check_remote
    }

    private fun label(text: String, size: Float, color: Int = Ink.LABEL, bold: Boolean = false) =
        this.label(text as CharSequence, size, color, bold)

    companion object {
        private const val REPAIR = "repair"
        private const val UPDATE = "update"
        private const val REPO = "goodwind/HyperVoice"
        const val EXTRA_NO_KEY = "no_key"
        const val EXTRA_KEY = "groq_key"
        const val EXTRA_PRESS_MIC = "press_mic"
        const val EXTRA_OPEN_SEARCH = "open_search"
    }
}
