# MIDI Chords

An Android application that displays musical notes and chord information in real-time from a connected MIDI device.

## Features

- Real-time MIDI note display on a musical staff
- Chord recognition and display
- Support for USB MIDI devices
- Sustain pedal support
- Portrait and landscape orientation support

## Requirements

- Android Studio Hedgehog (2023.1.1) or newer
- Android SDK 34
- Minimum Android version: Android 7.0 (API level 24)
- USB MIDI device

## Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Build and run the application

## Project Structure

```
app/
├── src/
│   └── main/
│       ├── java/com/midichords/
│       │   ├── midi/       # MIDI device handling
│       │   ├── model/      # Data models
│       │   ├── viewmodel/  # ViewModels
│       │   ├── view/       # UI components
│       │   └── util/       # Utility classes
│       └── res/           # Resources
```

## License

This project is licensed under the MIT License - see the LICENSE file for details. 