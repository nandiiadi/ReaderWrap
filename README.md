# ReaderWrap

A minimal Android WebView wrapper around Mozilla's actual Readability.js
(the library Firefox itself uses for Reader View). No extensions, no browser
flags — a self-contained app.

- Load a page in the WebView as normal.
- Tap the floating button: Readability.js parses the current DOM and swaps
  the page body for the cleaned article (title, byline, body content only).
- Tap again (or press back) to return to the original page.
- Registers a `VIEW` intent-filter for http/https, so **LinkSheet can offer
  this app directly as a browser target**, the same way it offers Titanium.
- Registers a `SEND` (text/plain) intent-filter, so you can also just
  "Share" a link into it from Telegram/WhatsApp/etc.

## Why this can't be built here

This was written in a sandboxed environment with no Android SDK and no
network access to Google's Maven repos — so it's source only, not a
compiled APK. Two ways to actually build it:

### Option A — Android Studio (simplest)
1. Open Android Studio -> **Open** -> select this `ReaderWrap` folder.
2. Let Gradle sync (it will fetch the Android Gradle Plugin, Kotlin plugin,
   and the three AndroidX/Material dependencies automatically).
3. Build -> Build APK(s), or just hit Run with a device/emulator connected.

### Option B — GitHub Actions (no local Android SDK needed)
Push this folder to a new repo and add a workflow like:

```yaml
name: build
on: [workflow_dispatch, push]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - run: gradle wrapper --gradle-version 8.7
      - run: ./gradlew assembleDebug
      - uses: actions/upload-artifact@v4
        with:
          name: app-debug-apk
          path: app/build/outputs/apk/debug/app-debug.apk
```
Download the artifact from the Actions run — that's your installable APK.

## Files
- `app/src/main/assets/readability.js` — unmodified copy of Mozilla's
  Readability.js (Apache 2.0, see `READABILITY_LICENSE.md` in the same folder).
- `app/src/main/assets/reader.css` — output styling (sepia/dark aware).
- `app/src/main/java/.../MainActivity.kt` — all the logic; ~140 lines, no
  other classes.

## Known limits (be aware, don't be surprised)
- No highlighting, TTS, notes, or any of Reader View's extras — this is
  intentionally just "strip clutter, show article."
- Like any Readability-based tool, it depends on the page having a
  recognizable article structure. Heavily templated/AMP pages sometimes
  parse poorly — same limitation r.jina.ai and every other Readability-based
  tool has, since they all use the same underlying heuristics.
- Not signed for release; `assembleDebug` gives you a debug-signed APK,
  fine for installing on your own device via ADB or direct install.
