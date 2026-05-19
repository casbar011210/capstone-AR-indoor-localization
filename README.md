[README.md](https://github.com/user-attachments/files/28010541/README.md)
# Hybrid Indoor Localization and AR Navigation System
## Based on Visual Localization and ARCore

## Overview

This project is a hybrid indoor localization and AR navigation system developed as a capstone research project.

The system combines:

- Visual localization
- ARCore motion tracking
- Android AR interaction
- Flask backend server
- Wi‑Fi communication
- Client-server architecture

to provide indoor positioning and navigation functionality in GPS-denied environments.

The project consists of two major components:

1. Android Client Application
2. Flask Localization Server

---

# System Architecture

```text
┌──────────────────────┐
│   Android Client     │
│  (ARCore + Camera)   │
└──────────┬───────────┘
           │
           │ HTTP / Wi‑Fi
           ▼
┌──────────────────────┐
│ Flask Backend Server │
│ (HLoc + COLMAP)      │
└──────────┬───────────┘
           │
           ▼
┌──────────────────────┐
│ Visual Localization  │
│ Coordinate Estimation│
└──────────────────────┘
```

---

# Main Technologies

| Technology | Purpose |
|---|---|
| Kotlin | Android development |
| Python 3.11 | Backend development |
| Flask | HTTP server |
| ARCore | Motion tracking and AR |
| Sceneform | AR rendering |
| Retrofit2 | Android networking |
| OkHttp | HTTP communication |
| COLMAP | Structure-from-Motion reconstruction |
| HLoc | Visual localization pipeline |
| ALIKED | Feature extraction |
| LightGlue | Feature matching |

---

# Android Client

The Android application provides:

- AR camera preview
- Mapping mode
- Localization mode
- Minimap visualization
- Real-time ARCore tracking
- Coordinate synchronization
- Real-time trajectory display

---

## Mapping Mode

During mapping mode:

1. The user moves through the indoor environment
2. The Android client continuously captures images
3. Images are uploaded to the Flask server through Wi‑Fi
4. The server reconstructs the environment map

---

## Localization Mode

During localization mode:

1. The user captures a query image
2. The server performs visual localization
3. The estimated pose is returned to the Android client
4. The minimap and AR position are updated

Example response:

```json
{
  "camera_position": [x, y, z],
  "quaternion": [x, y, z, w]
}
```

---

## AR Navigation

ARCore provides:

- Real-time pose estimation
- Device motion tracking
- Continuous coordinate updates

The system aligns ARCore coordinates with reconstructed localization coordinates to support smooth AR navigation.

---

# Flask Backend Server

The Flask backend server is responsible for:

- Image upload handling
- Feature extraction
- Feature matching
- COLMAP reconstruction
- Visual localization
- Coordinate estimation

---

## Localization Pipeline

```text
Image Upload
      │
      ▼
Feature Extraction (ALIKED)
      │
      ▼
Feature Matching (LightGlue)
      │
      ▼
COLMAP Reconstruction
      │
      ▼
Visual Localization
      │
      ▼
Pose Estimation
```

---

# Project Structure

```text
Project/
│
├── Android_Client/
│   ├── app/
│   ├── gradle/
│   └── README.md
│
├── Flask_Server/
│   ├── app.py
│   ├── hloc_service.py
│   ├── localize_service.py
│   └── README.md
│
└── README.md
```

---

# Environment Requirements

## Android Client

- Android Studio
- Android SDK 34+
- ARCore-supported Android device

## Flask Server

- Python 3.11
- CUDA-compatible GPU (recommended)

---

# Installation

## 1. Clone Repository

```bash
git clone https://github.com/your-repository-name.git
```

---

## 2. Android Client

Open the Android project using Android Studio and sync Gradle dependencies.

---

## 3. Flask Server

Create Python environment:

```bash
python -m venv capstone
```

Activate environment:

### Windows

```bash
capstone\Scripts\activate
```

### Linux / macOS

```bash
source capstone/bin/activate
```

Install dependencies:

```bash
pip install -r requirements.txt
```

Run Flask server:

```bash
python app.py
```

---

# Communication

The Android client communicates with the Flask server through HTTP APIs over the same Wi‑Fi network.

Example server address:

```text
http://192.168.x.x:5000
```

---

# Core Features

| Feature | Description |
|---|---|
| Mapping | Indoor image collection |
| Reconstruction | Visual map generation |
| Localization | Query image localization |
| AR Tracking | Real-time motion tracking |
| Minimap | Position visualization |
| Coordinate Alignment | Synchronize ARCore and map coordinates |

---

# Research Background

This project was developed for research in:

- Indoor localization
- Visual localization
- Mobile AR navigation
- Computer vision
- Client-server architecture

The system combines server-side visual localization with mobile AR tracking to improve localization accuracy and interaction smoothness.

---

# Future Improvements

- Multi-floor indoor navigation
- Improved localization robustness
- Real-time SLAM optimization
- Cloud deployment
- Better coordinate alignment
- Dynamic environment adaptation

---

# License

This project is for academic and research purposes.

