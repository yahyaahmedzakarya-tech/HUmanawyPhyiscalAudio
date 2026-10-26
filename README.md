# HUmanawyPhyiscalAudio

**Stream your Windows PC's audio to an Android phone over USB, built for old phones.**

Works on **Android 4.4.4 (KitKat) and up**. It's free and open source, so it's easy to learn from and modify.

## Why this exists

Apps like this already exist, but many are closed source or have dropped support for old Android versions. This project is open source and targets old phones (min SDK 19, no AppCompat or other heavy libraries), so a forgotten phone in a drawer can become a USB speaker for your PC.

## Features

- Works on Android 4.4.4+ (tested on a Samsung SM-J110F)
- Audio goes over USB cable, with no Wi-Fi needed
- Simple raw PCM protocol (easy to read and extend)
- Small codebase: one C# file on the PC, two Kotlin files on the phone
- Open source

## How it works

1. **PC** (`pc/`, C# + NAudio): captures a Windows playback device with WASAPI loopback and converts it to 16-bit PCM.
2. **USB tunnel**: `adb forward tcp:5000 tcp:5000` makes a TCP connection through the USB cable.
3. **Phone** (`android/`, Kotlin, min SDK 19): a foreground service listens on port 5000 and plays the stream with `AudioTrack`.

Protocol: 12-byte header (sample rate, channels, bits per sample as big-endian int32), then raw interleaved PCM.

## Requirements

- Windows 10 (or newer)
- [.NET SDK](https://dotnet.microsoft.com/download)
- [Android platform-tools](https://developer.android.com/tools/releases/platform-tools) (`adb`) on your PATH, or next to the program
- An Android phone, 4.4.4 or newer, with **USB debugging** enabled
- A data-capable USB cable (some cables only charge)

## How to use

1. Install the Android app (build `android/` with Android Studio, or `adb install app-debug.apk`).
2. Plug in the phone and check `adb devices` shows `device`.
3. Open the app on the phone and tap **Start receiver**.
4. On the PC:
   ```
   cd pc
   dotnet run
   ```
5. Pick the audio output device you want to stream, then play something into it.

## Known limitations

- Latency is about 100 to 200 ms on old Android versions, so it's great for music, less so for video or games.
- The PC program is a console app for now.
- WASAPI loopback sends nothing during silence, so you may hear a small click when audio resumes.

## Contributing

Issues and pull requests are welcome, especially reports of which old phones and Android versions work.

## License

MIT (see `LICENSE`)
