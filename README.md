# Clicker Counter

An Android app with two interactive canvas modes: freehand whiteboard drawing and tap-to-place animated pointer circles.

## Features

### Whiteboard Mode
Multi-touch freehand drawing on a canvas. Each finger draws its own independent stroke simultaneously.

### Pointer Mode
Tap anywhere to place an animated circle. Tapping near an existing circle (within 60 dp) grows it instead of creating a new one. Each circle fades out over 2 seconds and disappears automatically.

### Clear
Clears all strokes and circles instantly.

## Requirements

- Android 7.0 (API 24) or higher

## Build

Open the project in Android Studio and run on a device or emulator.

```bash
./gradlew assembleDebug
```

## Tech

- Kotlin
- Android SDK 35 (targets Android 15)
- Material 3
- Custom `View` with raw `Canvas` / `Paint` / `Path` — no external drawing library
- Multi-touch `MotionEvent` pointer ID tracking
- `ValueAnimator` for circle fade-out
