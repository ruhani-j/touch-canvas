# Touch Canvas

An Android app with three interactive canvas modes: freehand whiteboard drawing, a laser pointer that follows your finger, and a tap counter.

## Features

### Whiteboard Mode
Multi-touch freehand drawing on a canvas. Each finger draws its own independent stroke simultaneously. Pick from a color palette to draw in different colors.

### Pointer Mode
Touch the screen to show a laser pointer that follows your finger. Lift your finger and it fades out automatically.

### Counter Mode
Tap the screen to count taps. The running total is displayed large in the center. Multi-touch counts each finger separately. Use Clear to reset to zero.

### Clear
Clears all strokes or resets the counter, depending on the active mode.

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
- `Choreographer.FrameCallback` for pointer trail fade-out
