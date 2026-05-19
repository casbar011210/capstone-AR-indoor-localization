# Hybrid Indoor Localization and AR Navigation System
## Android Client

This Android application is the mobile client for a hybrid indoor localization and AR navigation system based on visual localization and ARCore.

The app communicates with a Flask backend server over Wi-Fi. It supports mapping mode, localization mode, ARCore tracking, minimap display, and coordinate alignment between ARCore coordinates and reconstructed map coordinates.

---

## Main Features

- AR camera preview
- Mapping mode for uploading environment images
- Localization mode for uploading query images
- Real-time ARCore motion tracking
- Minimap visualization
- Localization history display
- Dynamic backend server URL configuration

---

## Backend Server Configuration

When the app is opened for the first time, it will ask for the backend server URL.

Enter the server address shown in the Flask backend terminal, for example:

```text
http://192.168.1.100:5001/
```

Make sure:

- The Android device and backend server are connected to the same Wi-Fi network
- The Flask backend server is running before using mapping or localization mode
- The URL includes the correct port number

The server URL is saved automatically after input.

To change the backend server URL later, long press the top toolbar title area in the app.

---

## Technologies

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| ARCore | Real-time camera pose tracking |
| Sceneform | AR rendering framework |
| Retrofit2 | HTTP API communication |
| OkHttp | Network request handling |
| Flask Backend | Visual localization server |

---

## Project Structure

```text
Capstone1/
│
├── app/
│   ├── src/main/java/com/capstone/capstone/
│   ├── src/main/res/
│   └── build.gradle.kts
│
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
└── README.md
```

---

## How to Run

1. Open the project in Android Studio.
2. Sync Gradle dependencies.
3. Connect an ARCore-supported Android device.
4. Run the app on the device.
5. Enter the backend server URL when prompted.

---

## Notes

This project is designed for academic and research purposes. The Android client should be used together with the Flask backend server.
