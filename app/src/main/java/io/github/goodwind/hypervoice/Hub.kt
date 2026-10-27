package io.github.goodwind.hypervoice

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import io.github.goodwind.hypervoice.core.VoiceSession

/**
 * Who is listening. An app's own microphone button (SmartTube's, a keyboard's) starts a recognition session; the
 * remote's button, which no app handles, starts dictation. Only one at a time, and the button belongs to whoever
 * is already listening.
 */
object Hub {
    /** The session an app asked for through the recognition service, while it is listening. */
    @Volatile var recognition: VoiceSession? = null

    fun session(context: Context, listener: VoiceSession.Listener): VoiceSession =
        VoiceSession(RemoteMic(context), Settings(context).transcriber(), listener, now = SystemClock::elapsedRealtime, sleep = { Thread.sleep(it) })

    fun run(session: VoiceSession) {
        Thread(session::run, "hypervoice-session").start()
    }

    /** Opens the settings, saying why: there is no key. */
    fun openSettingsForKey(context: Context) {
        context.startActivity(
            Intent(context, SettingsActivity::class.java)
                .putExtra(SettingsActivity.EXTRA_NO_KEY, true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
