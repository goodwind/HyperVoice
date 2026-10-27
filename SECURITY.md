# Security

## What the app can do, and what it needs for it

HyperVoice is two Android services and a settings screen. What each can see and do:

- **Microphone.** It records from the voice remote only while a recognition is running: while you hold the remote's
  button (dictation), or while an app that asked for speech is listening. The clip is sent to Groq
  (`api.groq.com`) with your key and turned to text there. It is held in memory, not written to disk, and dropped
  afterwards. There is no analytics and nothing else is sent.
- **Accessibility service.** It is the only way an app can see the remote's microphone button (an unmapped key). It
  consumes that one key (scan code 63) and passes every other key on. To type dictated text it reads the text and
  cursor of the focused input field and sets the new text, and to press an app's microphone button it reads the
  accessibility tree of the window in front. It does not store or send what it reads.
- **Recogniser.** It answers apps that ask for speech recognition through Android's `RecognitionService`; any
  app may ask, as with Google's. What an app asks for is what the remote hears while it is listening.

## The setup page and updates

- **Setup page.** While the app's screen is open it listens on a random port of the TV's home-network address and
  serves one page for giving it the key. The address contains a random token made for that visit, is shown only in the
  QR code, and nothing else on that port answers. It is plain HTTP: anyone who can watch your home network while you
  use it can read the key. It stops when the screen closes.
- **`WRITE_SECURE_SETTINGS`.** Never asked of the person; if you give it once over `adb`, the app can turn on its own
  accessibility service and recogniser (and nothing else) when you press *Fix*.
- **Updates.** The app asks `api.github.com` for the latest release of `goodwind/HyperVoice`, takes only a file under
  that repository's own `releases/download`, refuses it unless its SHA-256 equals the digest GitHub announced for it,
  and hands it to Android's installer, which asks you to confirm and refuses a file not signed with the key of the
  copy it replaces. A release never carries a Groq key.

## The key

The Groq key is kept in the app's private storage (`SharedPreferences`), and is never logged. A build that was made
with `~/.cache/hypervoice/groq.key` present carries that key inside the APK, in clear text that anyone with the file can
read: such an APK is for your own TV, never for sharing or a release. The key is the only secret; the free tier is
limited, and a leaked key can be revoked at <https://console.groq.com/keys>.

## Limits you should know about

- Anything that can read the app's private files (root, a backup of the app data) can read the key. The app does not
  allow backups.
- The text goes to Groq, and so does the sound: do not dictate what you do not want a hosted service to hear.
- Whoever has the TV's `adb` access can switch the services on or off; that is the TV's own setting.

## Taking it back

- Uninstall HyperVoice (this deletes the key), and
- put `voice_recognition_service` and `enabled_accessibility_services` back to what they were (see the README), or
  turn the service off in the TV's accessibility settings.

## Reporting a vulnerability

Please do not open a public issue for a security problem. Use GitHub's
[private vulnerability reporting](https://github.com/goodwind/HyperVoice/security/advisories/new)
for this repository. You will get a first answer within about a week. If the report holds, a fix is
released and credited to you, if you wish.

Supported: the latest release.
