# Contributing

Thanks for looking. Issues and pull requests are welcome.

## Reporting a problem

Open an issue and tell us:

- the TV model and its Android / firmware version (`adb shell getprop ro.product.model`,
  `ro.build.version.release`) and the remote you use;
- what you did, what you expected and what happened;
- the lines the app writes while you try: `adb logcat -d -s HyperVoice` (they name the steps and the outcome, never
  the text you said and never the key).

Security problems: see [SECURITY](SECURITY.md), please not in a public issue.

## Translations

Strings are in `app/src/main/res/values*/strings.xml`: `values` is English, one folder per language. A new
language is a new `values-<code>` folder with the same keys; keep the `%1$s` / `%1$d` placeholders and use `’` rather
than a bare `'` in text. The languages Whisper can be primed with are in
[`Languages.kt`](app/src/main/java/io/github/goodwind/hypervoice/core/Languages.kt): a few commands in each, which is
what a hint needs. Better example commands for a language are a welcome change.

## Code

Kotlin. The app has one library beyond the Android platform: ZXing's core, for the QR code.

```bash
./gradlew testDebugUnitTest assembleDebug     # JDK 17, Android SDK platform 36
```

- Comments and identifiers in English. Every user-visible string in `values` (English) and, as you can, the other
  `values-*` folders.
- The `core` package is plain Kotlin with no Android in it, and it is where the logic lives: the Whisper client and
  its retries, where a phrase ends, typing into a field, a session from microphone to text. It is tested on the
  JVM. A change there comes with a test that failed before the change.
- The Android side (`RemoteMic`, the two services, the overlay, the settings) is thin on purpose and is checked on
  a TV. Say what you tested and on which TV, and list what you could not test.
- The key must never be logged or put in the repository.

## Releases

Maintainers sign them with a key kept outside the repository (`local/signing.properties`), tag `vX.Y.Z`, attach the
APK and its SHA-256, and add the release to [CHANGELOG](CHANGELOG.md). The app offers the latest release to the
people who have it installed, so a release is also an update.

## License

By contributing you agree that your contribution is licensed under the [Apache License 2.0](LICENSE).
