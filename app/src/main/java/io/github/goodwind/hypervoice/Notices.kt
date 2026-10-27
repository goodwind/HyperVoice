package io.github.goodwind.hypervoice

import android.content.Context
import io.github.goodwind.hypervoice.core.Outcome

/** One short sentence in the TV's language for what came of a phrase. */
fun Outcome.notice(context: Context): String = when (this) {
    is Outcome.Text -> text
    Outcome.NoSpeech -> context.getString(R.string.notice_no_speech)
    Outcome.Timeout -> context.getString(R.string.notice_timeout)
    Outcome.NoAudio -> context.getString(R.string.notice_no_audio)
    Outcome.NoKey -> context.getString(R.string.notice_no_key)
    Outcome.Unauthorized -> context.getString(R.string.notice_unauthorized)
    Outcome.RateLimited -> context.getString(R.string.notice_rate_limited)
    is Outcome.Server -> context.getString(R.string.notice_server, code)
    is Outcome.Network -> context.getString(R.string.notice_network)
}
