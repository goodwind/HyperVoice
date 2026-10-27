# HyperVoice

Voice search and dictation for Xiaomi / HyperOS TVs whose own voice assistant has been removed (or never had one
outside China), using the voice remote's microphone and Groq's hosted Whisper.

- **In apps.** It is the system's speech recogniser, in place of Google's. An app's microphone button (SmartTube's
  search, a keyboard's) asks it for text; it gives back what Whisper heard from the remote while its button was held.
- **Dictation.** The remote's microphone button reaches Android as an unmapped key (scan code 63) that no app handles.
  An accessibility service takes it: hold it, speak, and the text is typed into the focused field of any app. With no
  field in focus it presses the app's own microphone button (found by its view id, `lb_search_bar_speech_orb`, or its
  description), so the app handles the text its own way.

## How the remote's sound reaches an app

Xiaomi's Bluetooth service (`com.mi.bluetooth`) receives the remote's voice over BLE and, once something opens a
recording on the "wired headset" input whose address is the remote's, hands it to the audio HAL through a local socket.
So `AudioRecord` with that device preferred gets the remote's microphone, but only while its button is held. The stock
route (`com.xiaomi.voicecontrol`) is what most debloat scripts disable; this does not need it.

## Setup (adb)

```sh
adb install -r app-debug.apk
adb shell pm grant io.github.goodwind.hypervoice android.permission.RECORD_AUDIO
adb shell settings put secure voice_recognition_service io.github.goodwind.hypervoice/.HyperVoiceRecognitionService
# add the accessibility service to the ones already enabled:
adb shell settings put secure enabled_accessibility_services "<existing>:io.github.goodwind.hypervoice/.HyperVoiceAccessibilityService"
adb shell am start -n io.github.goodwind.hypervoice/.SettingsActivity --es groq_key gsk_...   # the key, once
```

The languages Whisper is primed with (English and the device's language by default) are chosen in the app.

## Key

The proof of concept reads a Groq key from `~/.cache/hypervoice/groq.key` at build time and puts it in the APK
(never in the repository). Setting it in the app, or from a phone, is still to do.

## Tests

`./gradlew testDebugUnitTest`: the parts that do not need a TV (WAV, multipart, the Whisper client and its retries,
where a phrase ends, typing into a field, the choice of languages, a session from microphone to text, spotting an
app's microphone button).
