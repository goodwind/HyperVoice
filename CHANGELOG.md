# Changelog

All notable changes to this project are documented here. The format follows
[Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and versions follow
[Semantic Versioning](https://semver.org/).

## [Unreleased]

Pre-release: not published yet, built from source.

### Added

- A system speech recogniser (`HyperVoiceRecognitionService`) that an app's microphone button asks for text:
  the remote's microphone through Whisper (Groq), in place of Google's.
- An accessibility service that takes the voice remote's microphone button (an unmapped key, scan code 63):
  hold it, speak, and the text is typed into the focused field of any app, at the cursor.
- With no field in focus, the service presses the app's own microphone button (Leanback's
  `lb_search_bar_speech_orb`, or a button whose description names voice input).
- An optional switch (off by default): with no field and no microphone button, press the app's search button,
  wait for the search field to take focus, and dictate into it.
- Whisper is primed with the languages the person chose: English and the language of the TV by default.
- A note over the screen for what is going on (*Listening…*, *Recognizing…*, or the failure), and the
  settings opened at once if there is no key.
- Retries: a short clip that gets an empty answer, a server error and a dropped connection are tried once
  more; a refused key and a spent limit are not.
- Interface in English and Russian.

### Known limits

- Needs the internet and a Groq key; the key is given with `adb` for now.
- The accessibility service and the recogniser are switched on with `adb`.
- Tested on one TV only: Xiaomi MiTV-MFTR0 (Android 11) with its Bluetooth voice remote.
