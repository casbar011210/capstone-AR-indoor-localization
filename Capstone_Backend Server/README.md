# Capstone AR Indoor Localization Server

## Overview

This project is the backend server for a hybrid indoor localization and AR navigation system.  
The server is implemented using Python and Flask, and communicates with an Android client through HTTP over Wi‑Fi.

The system supports:

- Indoor visual mapping
- Image-based localization
- 2D map reconstruction
- Real-time coordinate estimation
- AR-assisted navigation support

The backend combines modern visual localization techniques with lightweight mobile interaction to provide an indoor positioning solution for environments where GPS is unavailable.

---

## System Architecture

```text
Android Client
      │
      │  HTTP / Wi‑Fi Communication
      ▼
Flask Backend Server
      │
      ├── Image Upload
      ├── Feature Extraction
      ├── Feature Matching
      ├── COLMAP Reconstruction
      ├── Visual Localization
      └── Coordinate Output
```

---

## Main Technologies

| Technology | Purpose |
|---|---|
| Python 3.11 | Backend development |
| Flask | HTTP server and API |
| COLMAP | Structure-from-Motion reconstruction |
| HLoc | Hierarchical localization pipeline |
| ALIKED | Local feature extraction |
| LightGlue | Feature matching |
| SQLite | Local database storage |

---

## Project Structure

```text
flask_capstone/
│
├── app.py                     # Main Flask application
├── hloc_service.py            # Mapping and HLoc processing
├── localize_service.py        # Localization logic
├── db_models.py               # Database models
├── visualize_position.py      # Position visualization utility
│
├── datasets/                  # Uploaded mapping images
├── outputs/                   # Reconstruction outputs
├── query/                     # Query images for localization
├── uploads/                   # Temporary uploaded files
└── vps.db                     # SQLite database
```

---

## Core Functions

### 1. Mapping Mode

The Android client continuously uploads images while the user moves through the indoor environment.

The server:

1. Receives uploaded images
2. Extracts visual features
3. Matches image features
4. Reconstructs a 2D/3D visual map using COLMAP
5. Stores mapping information for localization

---

### 2. Localization Mode

The user captures a query image using the Android client.

The server:

1. Extracts features from the query image
2. Matches the query image with the reconstructed map
3. Estimates camera pose
4. Returns localization coordinates to the Android client

Example response:

```json
{
  "camera_position": [x, y, z],
  "quaternion": [x, y, z, w]
}
```

---

## API Example

### Upload Mapping Image

```http
POST /upload
```

### Localize Query Image

```http
POST /localize
```

### Get Map Data

```http
GET /path
```

---

## Environment Requirements

- Python 3.11.x
- CUDA-compatible GPU (recommended)
- Windows / Linux
- Android client connected through the same Wi‑Fi network

---

## Installation

### 1. Clone Repository

```bash
git clone https://github.com/your-repository-name.git
cd flask_capstone
```

### 2. Create Virtual Environment

```bash
python -m venv capstone
```

### 3. Activate Environment

#### Windows

```bash
capstone\Scripts\activate
```

#### Linux / macOS

```bash
source capstone/bin/activate
```

---

## Install Dependencies

```bash
pip install -r requirements.txt
```

---

## Run Server

```bash
python app.py
```

Default Flask server:

```text
http://0.0.0.0:5000
```

---

## Android Client

The Android client provides:

- AR camera preview
- Mapping mode
- Localization mode
- Minimap visualization
- ARCore motion tracking
- Real-time trajectory update

The client communicates with the Flask server using Retrofit and OkHttp.

---

## Research Background

This project was developed as part of a capstone research project focused on:

> Hybrid Indoor Localization Using Visual Localization, ARCore and Wi‑Fi Communication

The system combines server-side visual localization with mobile AR tracking to improve localization accuracy and real-time interaction.

---

## Future Improvements

- Real-time SLAM optimization
- Multi-floor indoor support
- Improved coordinate alignment
- Cloud deployment
- Better localization robustness in dynamic environments

---

## License

This project is for academic and research purposes.

