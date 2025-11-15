# Singing Bowl Tuner

## Project Goal

This project is a professional Android application designed to accurately measure the frequency (in Hz) and musical note of singing bowls. It's built to handle the unique sonic characteristics of singing bowls, such as strong overtones, long sustain, and initial frequency instability.

The primary goal is to create a high-precision tuner that works entirely offline, processing audio in real-time on the device.

## Tech Stack & Decisions

*   **Platform:** Native Android (Kotlin)
*   **Minimum SDK:** API 26 (Android 8.0)
*   **Core Libraries:**
    *   **Room:** For local database storage of bowl profiles.
    *   **Kotlin Coroutines:** For asynchronous operations and audio processing.

### Why Native Kotlin?

For this initial version (MVP), we chose Native Kotlin over cross-platform frameworks like Flutter for the following reasons:

1.  **Maximum Performance & Low Latency:** Native development provides direct access to the Android audio APIs (`AudioRecord`), which is critical for real-time digital signal processing (DSP). This minimizes latency between the moment the sound is captured and the frequency is displayed.
2.  **Precision:** Direct control over the audio processing pipeline ensures that we can implement and fine-tune complex DSP algorithms (like YIN or MPM) without the overhead or potential limitations of a cross-platform abstraction layer.
3.  **Stability:** A native implementation is often more stable and predictable for resource-intensive tasks like audio analysis.

### Future-Proofing for iOS

The application's architecture is designed to be modular. The core DSP logic for pitch detection is separated from the Android-specific UI and audio input layers. This modular design will allow the core DSP engine to be extracted into a shared library (e.g., a C++ library with JNI bindings) or rewritten in Swift/C++ for a future iOS version, reusing the fundamental algorithmic principles.

## Technical Summary

### Project Structure
The project is organized into the following key packages under `app/src/main/java/com/singingbowl/tuner`:
*   **(root)`/MainActivity.kt`**: The main UI controller.
*   `audio`/`AudioInput.kt`: Handles microphone access and real-time audio stream buffering.
*   `dsp`/`PitchDetector.kt`: Contains the core YIN pitch detection algorithm.
*   `data`/`: Contains the Room database components (`BowlProfile`, `BowlProfileDao`, `AppDatabase`).
*   `utils`/`PitchConverter.kt`: A utility for converting frequency (Hz) to musical notes and cents.

### Core Logic Location
*   **Audio Capture**: The `AudioInput.kt` class is responsible for configuring and managing `AudioRecord`. It continuously reads audio data into a buffer, which is exposed as a `StateFlow` for other parts of the app to consume.
*   **Pitch Detection**: The `PitchDetector.kt` class implements the YIN algorithm. Its `getPitch(audioBuffer)` method takes a raw audio buffer (`ShortArray`) and returns the estimated fundamental frequency.

### Algorithm Parameters
Several key parameters in the code can be adjusted to fine-tune the tuner's performance:
*   `AudioInput.kt`:
    *   `SAMPLE_RATE`: Currently hardcoded to `44100` Hz. Can be changed to other standard rates like `48000` Hz if needed.
    *   `CHANNEL_CONFIG`: Set to `CHANNEL_IN_MONO`.
    *   `AUDIO_FORMAT`: Set to `ENCODING_PCM_16BIT`.
*   `PitchDetector.kt`:
    *   `threshold`: The YIN algorithm's confidence threshold, currently `0.15f`. Lowering it can make the detector more sensitive but may increase errors with noisy signals. Increasing it makes the detector more confident but may cause it to miss notes.
*   `MainActivity.kt`:
    *   `historySize`: The size of the moving average window for smoothing the frequency output. Currently set to `5`. Increasing this value will make the frequency display smoother but increase latency.
    *   `deviation` threshold in `smoothFrequency()`: The heuristic for signal stability, currently `< 1.0`.

### Known Limitations (MVP)
*   **No Overtone Analysis**: The current implementation focuses solely on detecting the fundamental frequency. It does not analyze or display overtones.
*   **Fixed A4 Reference**: The A4 reference pitch is currently hardcoded to 440 Hz in `PitchConverter.kt`. A UI for changing this is not yet implemented.
*   **No Advanced Noise Reduction**: The app relies on the YIN algorithm's robustness and basic smoothing. No advanced noise filtering (like a low-pass filter) is currently implemented.
*   **Basic UI**: The list of saved bowls is not yet implemented in the UI. Data is saved to the database but not displayed.

## How It Works: The YIN Algorithm

This tuner uses the **YIN algorithm**, a robust method for fundamental frequency estimation, making it well-suited for the complex sounds of singing bowls. Here's a simplified overview of how it works:

1.  **Difference Function**: Instead of traditional autocorrelation, YIN calculates a modified difference function. This helps in reducing errors caused by strong overtones, which are common in singing bowls.
2.  **Cumulative Mean Normalization**: The result is then normalized to prevent errors in picking octave multiples of the fundamental frequency.
3.  **Absolute Threshold**: YIN searches for the first dip in the function that goes below a specific confidence threshold. This dip corresponds to the period of the fundamental frequency.
4.  **Parabolic Interpolation**: To improve accuracy, the algorithm uses parabolic interpolation to find a more precise estimate of the period.

This method allows the tuner to isolate the primary note (fundamental) even when the sound is rich with harmonics and overtones, which would confuse simpler tuning algorithms.

## Tuning Tips for Best Results

1.  **Minimize Background Noise**: For the most accurate reading, perform the tuning in a quiet environment.
2.  **Position the Microphone**: Place your Android device close to the singing bowl (about 15-30 cm away) but not touching it.
3.  **Use a Soft Mallet**: Strike the bowl gently on its side with a soft, felt-covered mallet. A hard strike or a wooden mallet can create harsh initial transients and excessive overtones that may confuse the algorithm.
4.  **Wait for the Sustain**: The most stable and pure tone of a singing bowl emerges a moment after the initial strike. Let the tone ring for a second or two before relying on the measurement. The "Тон стабилен" (Signal is stable) indicator will appear when the sound has stabilized.
5.  **Avoid Hand Movements**: Hold the bowl in the palm of your hand and try to keep your hand still during the measurement to avoid dampening the sound or creating extraneous noise.

## How to Build and Run

### Prerequisites

*   Android Studio (latest version recommended)
*   An Android device or emulator running Android 8.0 (API 26) or higher

### Steps

1.  **Clone the repository:**
    ```bash
    git clone <repository-url>
    ```
2.  **Open the project in Android Studio:**
    *   Start Android Studio.
    *   Select "Open an existing project".
    *   Navigate to the cloned repository folder and select it.
3.  **Build the project:**
    *   Wait for Android Studio to sync the Gradle files.
    *   Click on `Build > Make Project` from the top menu.
4.  **Run the application:**
    *   Select your target device (emulator or physical device) from the dropdown menu.
    *   Click the "Run" button (green play icon).

## How to Build and Install the APK

### Building the APK

You can build a release APK file directly from the command line using Gradle.

1.  **Open a terminal or command prompt** in the root directory of the project.
2.  **Run the following command:**

    ```bash
    ./gradlew assembleRelease
    ```
    *(On Windows, you might need to use `gradlew.bat assembleRelease`)*

3.  Once the build is complete, you will find the generated APK file in the following directory:
    `app/build/outputs/apk/release/app-release.apk`

### Installing the APK on a Device

1.  **Enable USB Debugging** on your Android device. You can find this option in `Settings > Developer options`.
2.  **Connect your device** to your computer via a USB cable.
3.  **Copy the APK file** (`app-release.apk`) to your device.
4.  **Install the APK:**
    *   Open a file manager on your device and navigate to where you copied the APK.
    *   Tap on the `app-release.apk` file.
    *   You may be prompted to allow installation from "unknown sources". Please enable this to proceed.
    *   Confirm the installation.

Once installed, you can find the "Singing Bowl Tuner" app in your app drawer.

## How to Use the Application

1.  **Grant microphone permission:** When you first launch the app, it will ask for permission to record audio. This is necessary for the tuner to work.
2.  **Start measuring:** Once permission is granted, the app will immediately start listening for sound.
3.  **Strike your singing bowl:** Strike the bowl near your device's microphone. The tuner will display:
    *   The detected musical note (e.g., "A3").
    *   The precise frequency in Hertz (Hz).
    *   The deviation from the perfect note in cents.
4.  **Wait for stability:** For the most accurate reading, wait for the "Тон стабилен" (Signal is stable) message to appear.
5.  **Save the profile:** Once the signal is stable, the "Сохранить" (Save) button will become active. Click it to save the measurement. You will be prompted to enter a name for your bowl.

## Progress

*   **[DONE]** Project Initialization & Setup
*   **[DONE]** Real-time Audio Input
*   **[DONE]** Pitch Detection Algorithm (YIN)
*   **[DONE]** Frequency-to-Note Conversion
*   **[DONE]** Tuner UI Implementation
*   **[DONE]** Bowl Profile Saving (Room DB)
*   **[DONE]** Finalization & Documentation
