<div align="center">

<img src="docs/img/icon.png" width="112" alt="HyperVoice icon">

# HyperVoice

**Voice search and dictation for HyperOS TVs.**

Hold the microphone button on the remote and speak: the text goes into the search box or any text field
of any app. For TVs whose firmware has no working voice assistant. No root.

[![License: Apache-2.0](https://img.shields.io/badge/license-Apache--2.0-blue.svg)](LICENSE)
![Android 7.0+](https://img.shields.io/badge/Android-7.0%2B-3ddc84?logo=android&logoColor=white)
![Status: pre-release](https://img.shields.io/badge/status-pre--release-orange)

**English** · [Русский](README.ru.md)

</div>

## Why

Xiaomi's TVs sold with a voice remote answer it with a voice assistant that is Chinese-only, and people who
clean the firmware up (or live outside China) end up without it. Apps have no help either: the microphone
button in an app's search, in a keyboard, in any TV app, asks Android's speech recogniser, and a TV
without Google's services has none that works. So the remote has a microphone and nobody listens to it.

HyperVoice listens to it.

## What it does

- **In apps.** It is the system's speech recogniser, in place of Google's. An app's own microphone button
  asks it for text, and it gives back what Whisper heard from the remote while its button was held.
- **Dictation.** Hold the remote's microphone button, speak, let go: the text is typed into the field that
  has focus, in any app, at the cursor.
- **No field in focus?** It presses the app's own microphone button for you (found by its accessibility id or
  description), so the app handles the text its own way, a search box for one.
- **No microphone button either?** Optionally (a switch in the app) it presses the app's search button, waits
  for the search field to take focus, and types what you said into it.
- **Several languages in one phrase.** Whisper is told which languages to expect (English and the language of
  the TV by default; you choose in the app), so "включи клип Imagine Dragons" comes out as written.
- A small note over the screen says what is going on: *Listening…*, *Recognizing…*, or what went wrong.

## Install

There is no release yet. Build it (see [Build](#build)) and install over the network:

```bash
adb connect <tv-ip>:5555
adb install app-debug.apk
adb shell pm grant io.github.goodwind.hypervoice android.permission.RECORD_AUDIO

# Make it the recogniser, and turn its accessibility service on next to the ones already on:
adb shell settings put secure voice_recognition_service io.github.goodwind.hypervoice/.HyperVoiceRecognitionService
adb shell settings get secure enabled_accessibility_services
adb shell settings put secure enabled_accessibility_services "<what the line above printed>:io.github.goodwind.hypervoice/.HyperVoiceAccessibilityService"
```

Then give it a Whisper key (below) and open **HyperVoice** from the TV's app list to see its state and choose
the languages.

To go back to what you had, put the two settings back to their earlier values.

### The key

Speech is turned to text by Whisper on [Groq](https://groq.com). It needs a key, free for personal use within
Groq's limits: sign in at <https://console.groq.com>, open **API Keys**, create one. Give it to the app once:

```bash
adb shell am start -n io.github.goodwind.hypervoice/.SettingsActivity --es groq_key gsk_...
```

Setting it from a phone is planned. Keep the key out of anything you publish.

### Privacy

Sound is recorded only while a recognition is running, that is while you hold the remote's button, and the
clip is sent to Groq to be turned to text. Nothing else leaves the TV, there is no analytics, and the clip is
not kept. See [SECURITY](SECURITY.md).

## How it works

Xiaomi's Bluetooth service receives the remote's voice and, once something records from the "wired headset"
input that Android lists for the remote, hands it to the audio driver. So an ordinary `AudioRecord` with that
device preferred gets the remote's microphone, but only while its button is held. The button itself reaches
Android as an unmapped key (scan code 63), which no app handles; an accessibility service does. The stock route
(`com.xiaomi.voicecontrol`) is what cleanup scripts turn off, and HyperVoice does not need it.

More in [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

## What it cannot do

- **Work offline.** It needs the internet and a Groq key. An offline model was tried and was far less
  accurate; it is not included.
- **Work with any remote.** It relies on how Xiaomi's voice remote and Bluetooth service present the
  microphone. Other TVs and remotes are untested.
- **Turn itself on.** The accessibility service and the recogniser are switched on with `adb` as above,
  because Android lets an app do neither on its own.
- **Press a microphone button an app does not label.** The accessibility id of Leanback's (`lb_search_bar_speech_orb`)
  and a few words in the button's description are what it looks for.

## Tested on

- Xiaomi MiTV-MFTR0 (Android 11) with its Bluetooth voice remote: dictation into a field, and the microphone
  button in an app's search.

Reports from other TVs are welcome.

## Build

JDK 17 and the Android SDK (platform 36):

```bash
./gradlew testDebugUnitTest assembleDebug
```

The APK is in `app/build/outputs/apk/debug/`. A build can carry a key for your own use: put it in
`~/.cache/hypervoice/groq.key` before building and it is baked into the APK (never into the repository).
Do not share an APK built that way.

## License

[Apache License 2.0](LICENSE). Third-party material is listed in [NOTICE](NOTICE).
