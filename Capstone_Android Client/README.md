# Capstone AR Indoor Localization Android Client

## Overview

This project is the Android client application for a hybrid indoor localization and AR navigation system.

The application is developed using Kotlin and Android Studio, and communicates with a Flask backend server through HTTP over Wi‑Fi.

The Android client provides:

- Indoor mapping mode
- Visual localization mode
- AR camera preview
- Real-time ARCore tracking
- Minimap visualization
- Coordinate synchronization
- Real-time trajectory display

The system combines ARCore motion tracking with server-side visual localization to provide indoor navigation functionality in GPS-denied environments.

---

## System Workflow

```text
User Movement
      │
      ▼
Android Camera Capture
      │
      ├── Mapping Mode → Upload Images to Server
      │
      └── Localization Mode → Query Image Upload
                                      │
                                      ▼
                           Flask Localization Server
                                      │
                                      ▼
                          Localization Coordinates
                                      │
                                      ▼
                     AR Navigation & Minimap Update
```

---

## Main Technologies

| Technology | Purpose |
|---|---|
| Kotlin | Android application development |
| Android Studio | Development environment |
| ARCore | Motion tracking and AR visualization |
| Sceneform | 3D rendering framework |
| Retrofit2 | HTTP communication |
| OkHttp | Network requests |
| OpenGL / AR Rendering | Real-time visualization |

---

## Main Features

### 1. Mapping Mode

The application continuously captures and uploads indoor environment images while the user moves around the environment.

Functions:

- Real-time image upload
- Mapping status display
- Camera preview
- Wi‑Fi communication with server
- Indoor environment reconstruction support

---

### 2. Localization Mode

The user captures a query image for localization.

The server processes the image and returns:

- Camera position
- Pose estimation
- Rotation quaternion

The Android client then:

- Updates the minimap
- Displays localization history
- Synchronizes ARCore tracking
- Updates current user position

---

### 3. AR Navigation

ARCore is used to provide:

- Real-time pose estimation
- Motion tracking
- Camera coordinate updates
- Smooth localization visualization

The reconstructed localization coordinates are aligned with ARCore coordinates to support continuous tracking.

---

## Project Structure

```text
Capstone/
│
├── app/
│   ├── src/main/java/
│   ├── src/main/res/
│   ├── AndroidManifest.xml
│   └── build.gradle
│
├── gradle/
├── build.gradle
├── settings.gradle
└── README.md
```

---

## Environment Requirements

- Android Studio
- Android SDK 34+
- Kotlin
- ARCore-supported Android device
- Flask localization server running in the same Wi‑Fi network

Recommended test devices:

- Samsung S25 Ultra
- Google Pixel series

---

## Installation

### 1. Clone Repository

```bash
git clone https://github.com/your-repository-name.git
```

---

### 2. Open Project

Open the project using Android Studio.

---

### 3. Sync Gradle

Allow Android Studio to download dependencies automatically.

---

### 4. Run Application

Connect an ARCore-supported Android device and run the application.

---

## Server Connection

The Android client communicates with the Flask backend server through HTTP APIs.

Example:

```text
http://192.168.x.x:5000
```

The device and server must be connected to the same Wi‑Fi network.

---

## Core Functions

| Function | Description |
|---|---|
| Mapping | Upload environment images |
| Localization | Query image localization |
| AR Tracking | Real-time device tracking |
| Minimap | Display trajectory and position |
| Coordinate Alignment | Synchronize ARCore and map coordinates |

---

## Research Background

This project was developed as part of a capstone research project:

> Hybrid Indoor Localization Based on Visual Localization, ARCore and Wi‑Fi Communication

The system combines:

- Server-side visual localization
- ARCore motion tracking
- Mobile AR interaction
- Wi‑Fi communication

to provide a lightweight indoor navigation solution.

---

## Future Improvements

- Multi-floor navigation
- Improved localization robustness
- Real-time SLAM optimization
- Cloud synchronization
- Better UI/UX design
- Dynamic environment adaptation

---

## License

This project is for academic and research purposes.

