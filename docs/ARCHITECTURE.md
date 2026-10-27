# Architecture

Kotlin, two Gradle modules: `shared` (how a request gets out, versions and the update check, the checked download and
the hand-over to Android's installer: the same code HyperMissions carries) and `app`. One library beyond the Android
platform: ZXing core, for the QR code. The logic is plain Kotlin in `core` and is tested on the JVM; the Android side
around it is thin.

```
app/src/main/java/io/github/goodwind/hypervoice/
├── core/                         no Android here; the unit tests
│   ├── VoiceSession.kt           one phrase: open the microphone, listen, find the end, send, report
│   ├── Endpointing.kt            where a phrase begins and ends, from when audio arrives
│   ├── WhisperClient.kt          Groq's Whisper over HTTPS, with its retries
│   ├── Transport.kt              how a request gets out (faked in tests), HttpTransport the real one
│   ├── Multipart.kt              the multipart/form-data body, built by hand
│   ├── Wav.kt                    PCM to WAV, and the room of silence round a short clip
│   ├── Outcome.kt                what came of a phrase, and which SpeechRecognizer error it is
│   ├── TextInsertion.kt          typing into a field that holds text; tidying a transcript (Dictation)
│   ├── Languages.kt              the languages Whisper can be primed with, and the hint made from them
│   ├── MicButton.kt              telling an app's microphone button from the rest of its screen
│   ├── SearchButton.kt           the same for an app's search button
│   ├── KeyPlan.kt                what the remote's button does: dictate, press the mic button, open the search, or ask for the key
│   ├── KeyCheck.kt               whether a Groq key is accepted (the list of models, which costs nothing)
│   ├── SetupStatus.kt            what is in place and what the Fix button can put right
│   ├── SetupWeb.kt               the key page's routes and rules, with no sockets
│   ├── HttpWire.kt               just enough HTTP/1.1 for that page
│   └── AccessibilityList.kt      adding a service to the secure setting without disturbing the others
├── RemoteMic.kt                  AudioRecord on the remote's microphone
├── Hub.kt                        who is listening: an app's session, or dictation
├── HyperVoiceRecognitionService.kt   the system speech recogniser, for apps' microphone buttons
├── HyperVoiceAccessibilityService.kt the remote's microphone button; typing into the focused field
├── Overlay.kt                    the small note over the screen
├── Settings.kt                   the key, the languages and the two options
├── SettingsActivity.kt           the header (state, Fix, QR, update), the options, the footer
├── LanguagesActivity.kt          the languages, as sliders
├── SetupServer.kt, SetupPage.kt  the sockets and the page a phone gets (in the app's 20 languages)
├── Repair.kt                     reads the state of the setup, and puts the two secure settings right
├── Qr.kt, Network.kt             the QR code, and the TV's address on the home network
├── Ui.kt                         the palette and the widgets (focus rows, sliders, progress pill)
└── Notices.kt                    an Outcome as a sentence in the TV's language
```

## How the remote's sound reaches an app

On Xiaomi's TVs the voice remote talks BLE to `com.mi.bluetooth`. When its button goes down that service starts
collecting the voice (about 64 kbit/s) in a queue. A local socket (`com.mi.bluetooth.MiVoiceControl.VoiceScoket`)
is how the queue is drained: the audio driver connects to it when something starts recording from the input that
Android lists as a *wired headset* whose address is the remote's, and from then on the voice is the content of that
recording. So an `AudioRecord` from the ordinary microphone with that device preferred (`setPreferredDevice`) hears
the remote, and only while its button is held; before the button there is silence, which is why a session waits for
audio rather than for the clock.

The stock consumer of all this, `com.xiaomi.voicecontrol`, is what cleanup scripts disable. Nothing here needs it;
it is only that with it gone, nobody was recording, and the voice was dropped (the queue overflows).

The button itself is a key event with scan code 63 and key code `KEYCODE_UNKNOWN` from the remote's keyboard device.
No app handles it. Android's accessibility key filter (`canRequestFilterKeyEvents`) hands it to
`HyperVoiceAccessibilityService`, which takes it and passes every other key on.

## A phrase

`VoiceSession.run` blocks, so each phrase runs on a thread of its own:

1. open the microphone (`RemoteMic.start`), wait; a first chunk of audio is the start of speech;
2. keep every chunk, and report its level (the microphone animation of an app uses it);
3. the phrase ends when the caller says so (`finish`, the button was let go) or audio stops for half a second
   (`Endpointing`); no audio at all for eight seconds is a timeout; twenty seconds is cut off;
4. anything under a quarter of a second is a bounced button and is not sent;
5. the rest is padded with 0.3 s of silence each side (a one second clip that began on its first word got an empty
   answer from Whisper), made a WAV, and sent. An empty answer, a server error and a lost connection are tried once
   more; a refused key and a spent limit are not.

## Two ways in, one microphone

`Hub.recognition` is the session an app asked for. While it exists, the remote's button belongs to the app: the
accessibility service only ends its phrase when the button is let go. Otherwise the button starts dictation:

- a focused, editable field takes the text at its cursor (`TextInsertion`), one sentence without its full stop;
- else, if the window has an app's own microphone button, it is pressed (`MicButton` decides what is one), and the
  app starts a session through the recogniser, which is the first case;
- else, with the *open search* option on and a search button in the window, that is pressed and the text goes to the
  field that takes focus;
- else nothing happens: no sound, no note, no clip sent (`KeyPlan` decides).

## Why Whisper

Offline recognition (Vosk, a small Russian model) was tried first and measured against the same 19 phrases read by
one person into the remote (12 English, 5 mixed, 2 Russian): about 80% of the words wrong in English and Russian-only
phrases mixed with English, against about 3% for Whisper large-v3-turbo with a two-language hint. One speaker and one
room, so it shows the gap, not a figure to rely on. Whisper also keeps `Imagine Dragons` and `YouTube` in Latin
letters inside a Russian sentence, which a one-language model cannot.

The `language` setting of Whisper takes one language, and a phrase often has two, so the languages are given in a
prompt (`Languages.hint`) with a few commands in each. A hint in Russian only pushed English speech to Russian text;
the two-language hint did not.
