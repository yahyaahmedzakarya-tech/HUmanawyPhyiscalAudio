# HUmanawyPhyiscalAudio

Streams audio from a Windows PC to an Android phone over USB (a SoundWire-style tool).

- `pc/`: Windows console app (C#, NAudio). Captures audio with WASAPI loopback and sends it over a TCP socket through `adb forward`.
- `android/`: Android receiver app (Kotlin, min SDK 19). Plays the stream with AudioTrack.

## How to use
1. Install the .NET SDK and Android platform-tools (adb).
2. Enable USB debugging on the phone and install the Android app.
3. Open the app and tap "Start receiver".
4. In `pc/`, run `dotnet run` and pick your audio output device.