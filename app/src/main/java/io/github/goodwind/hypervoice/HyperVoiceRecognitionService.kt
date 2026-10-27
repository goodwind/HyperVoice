package io.github.goodwind.hypervoice

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.RemoteException
import android.util.Log
import android.speech.RecognitionService
import android.speech.SpeechRecognizer
import io.github.goodwind.hypervoice.core.Dictation
import io.github.goodwind.hypervoice.core.Outcome
import io.github.goodwind.hypervoice.core.VoiceSession

/**
 * The system's speech recogniser, in place of Google's: an app's microphone button (a search box's) comes here,
 * and the text it is given is what Whisper heard from the remote while its button was held.
 */
class HyperVoiceRecognitionService : RecognitionService() {
    private val main = Handler(Looper.getMainLooper())
    private var session: VoiceSession? = null

    override fun onStartListening(recognizerIntent: Intent, callback: Callback) {
        Log.i(TAG, "an app started listening")
        session?.cancel()
        if (!Settings(this).hasKey) {
            main.post { Hub.openSettingsForKey(this) }
            tell { callback.error(Outcome.NoKey.recognizerError()!!) }
            return
        }
        val mine = Hub.session(this, object : VoiceSession.Listener {
            override fun onReady() = tell { callback.readyForSpeech(Bundle()) }
            override fun onSpeechStarted() = tell { callback.beginningOfSpeech() }
            override fun onLevel(rms: Double) = tell { callback.rmsChanged((rms / 3000.0 * 10).coerceAtMost(10.0).toFloat()) }
            override fun onSpeechEnded() = tell { callback.endOfSpeech() }
            override fun onResult(outcome: Outcome) {
                Log.i(TAG, "recognition result: ${if (outcome is Outcome.Text) "text, ${outcome.text.length} characters" else outcome}")
                Hub.recognition = null
                tell {
                    when (outcome) {
                        is Outcome.Text -> callback.results(
                            Bundle().apply {
                                putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, arrayListOf(Dictation.clean(outcome.text)))
                                putFloatArray(SpeechRecognizer.CONFIDENCE_SCORES, floatArrayOf(1f))
                            },
                        )
                        else -> callback.error(outcome.recognizerError() ?: SpeechRecognizer.ERROR_CLIENT)
                    }
                }
                if (outcome == Outcome.NoKey || outcome == Outcome.Unauthorized) main.post { Hub.openSettingsForKey(this@HyperVoiceRecognitionService) }
            }
        })
        session = mine
        Hub.recognition = mine
        Hub.run(mine)
    }

    override fun onStopListening(callback: Callback) {
        Log.i(TAG, "the app stopped listening")
        session?.finish()
    }

    override fun onCancel(callback: Callback) {
        session?.cancel()
        session = null
        Hub.recognition = null
    }

    override fun onDestroy() {
        session?.cancel()
        Hub.recognition = null
        super.onDestroy()
    }

    /** The client is told on the main thread; one that has gone away is not an error here. */
    private fun tell(call: () -> Unit) {
        main.post {
            try { call() } catch (_: RemoteException) { }
        }
    }

    private companion object { const val TAG = "HyperVoice" }
}
