# Face Verification App (Android)

An Android application that uses face verification/recognition as a biometric lock mechanism for the device or for specific apps — combining a camera-based face check with an accessibility-service-driven app locker.

## Overview

Based on the app's manifest and component structure, the project is made up of:

- **MainActivity** — the app's entry point/launcher screen.
- **FaceVerificationActivity** — handles camera capture and face verification logic.
- **AppLockerService** — a foreground service that manages the app-locking behavior in the background.
- **AppLockerAccessibilityService** — an Android Accessibility Service that monitors foreground app usage (to detect when a locked app is opened) and can trigger the face verification flow before granting access.

Together, these components implement a flow where selected apps (or the device itself) are protected behind a face-verification check: the accessibility service detects when a protected app is launched, and the face verification activity is shown to confirm the user's identity before allowing access.

## Repository Structure

```
Faceverification_app/
├── assets/                                  # App assets (e.g. models, images)
├── java/com/example/myapplication/          # Application source code
│   ├── MainActivity                         # App entry point
│   ├── FaceVerificationActivity             # Face capture & verification screen
│   ├── AppLockerService                     # Foreground service for app-lock behavior
│   └── AppLockerAccessibilityService        # Accessibility service to detect foreground app changes
├── res/                                     # Android resources (layouts, strings, styles, icons)
├── AndroidManifest.xml                      # App manifest — permissions, activities, services
├── LICENSE                                  # MIT License
└── README.md
```

## Permissions Used

The app requests a broad set of permissions to support its functionality:

- **Camera** (`CAMERA`) — for face capture during verification.
- **Storage / Media** (`READ_EXTERNAL_STORAGE`, `WRITE_EXTERNAL_STORAGE`, `READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`) — for reading/writing images and media.
- **Foreground Service** (`FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`, `FOREGROUND_SERVICE_SPECIAL_USE`) — to keep the app-locker service running in the background.
- **Accessibility Service** (`BIND_ACCESSIBILITY_SERVICE`) — to monitor which app is currently in the foreground, enabling the app-lock trigger.
- **Package Usage Stats** (`PACKAGE_USAGE_STATS`) — to detect app launches/usage.
- **Notifications** (`POST_NOTIFICATIONS`) — for Android 13+ notification support.
- **Battery Optimization** (`REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`) — to keep the background service alive.

> ⚠️ Several of these permissions (Accessibility Service, Package Usage Stats, foreground services) are sensitive and require explicit user grant through system settings, not just the manifest — the app must guide users to enable them manually.

## Requirements

- Android Studio (recent version)
- Android SDK
- A physical Android device or emulator with a camera (for testing face verification)

## Getting Started

1. Clone the repository:
   ```bash
   git clone https://github.com/Ahsantw/Faceverification_app.git
   ```
2. Open the project in Android Studio.
3. Let Gradle sync and resolve dependencies.
4. Build and run the app on a device or emulator.
5. On first launch, grant the requested permissions, and manually enable:
   - **Accessibility Service** access (Settings → Accessibility → enable the app's service).
   - **Usage Access** (Settings → Apps → Special access → Usage access).
   - **Ignore battery optimizations** for the app, so the locker service isn't killed in the background.

## How It Works

1. The **AppLockerAccessibilityService** continuously monitors which app is in the foreground.
2. When a protected app is detected, it triggers the **FaceVerificationActivity**.
3. The activity captures the user's face via the camera and verifies it against a registered/reference face.
4. On successful verification, access to the app is granted; otherwise, access is blocked.
5. The **AppLockerService** keeps this monitoring active as a foreground service.

## License

This project is licensed under the **MIT License**. See the [LICENSE](https://github.com/Ahsantw/Faceverification_app/blob/main/LICENSE) file for details.
