package io.github.goodwind.hypervoice

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import io.github.goodwind.hypervoice.core.AudioSource

/**
 * The microphone of the voice remote. Xiaomi's Bluetooth service hands the remote's audio to the audio HAL, which
 * Android lists as a wired headset whose address is the remote's; asked for the ordinary microphone with that device
 * preferred, the sound comes only while the remote's button is held.
 */
class RemoteMic(private val context: Context) : AudioSource {
    private var record: AudioRecord? = null

    override fun start() {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val remote = audio.getDevices(AudioManager.GET_DEVICES_INPUTS)
            .firstOrNull { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET && !it.address.isNullOrEmpty() }
        val minimum = AudioRecord.getMinBufferSize(RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val r = AudioRecord(MediaRecorder.AudioSource.MIC, RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, maxOf(minimum, RATE) * 2)
        check(r.state == AudioRecord.STATE_INITIALIZED) { "the microphone did not open" }
        if (remote != null) r.setPreferredDevice(remote)
        r.startRecording()
        record = r
    }

    override fun read(buffer: ShortArray): Int {
        val n = record?.read(buffer, 0, buffer.size, AudioRecord.READ_NON_BLOCKING) ?: 0
        return if (n > 0) n else 0
    }

    override fun stop() {
        val r = record ?: return
        record = null
        try { r.stop() } catch (_: IllegalStateException) { }
        r.release()
    }

    companion object {
        const val RATE = 16_000

        /** Whether the remote's microphone is listed as an input now (it is not while the remote sleeps). */
        fun present(context: Context): Boolean =
            (context.getSystemService(Context.AUDIO_SERVICE) as AudioManager).getDevices(AudioManager.GET_DEVICES_INPUTS)
                .any { it.type == AudioDeviceInfo.TYPE_WIRED_HEADSET && !it.address.isNullOrEmpty() }
    }
}
