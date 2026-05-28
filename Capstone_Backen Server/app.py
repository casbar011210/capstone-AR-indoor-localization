import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import json
import sys
import shutil
import importlib
from pathlib import Path

import numpy as np
from flask import Flask, request, jsonify
from PIL import Image
from datetime import datetime

from db_models import Session

# ========= Path Initialization =========
HERE = Path(__file__).resolve().parent
PROJECT_ROOT = HERE.parent
sys.path.insert(0, str(PROJECT_ROOT))

session = Session()

# ========= Reload Modules =========
if 'hloc_service' in sys.modules:
    del sys.modules['hloc_service']
if 'localize_service' in sys.modules:
    del sys.modules['localize_service']

hloc_service = importlib.import_module('hloc_service')
localize_service = importlib.import_module('localize_service')

from hloc_service import OUTPUTS, DB_DIR, SFM_DIR, FEATURES, MATCHES
from hloc.utils.read_write_model import read_model

rebuild = hloc_service.rebuild
localize_image = localize_service.localize_image

# ========= Log File Path =========
LOG_PATH = HERE / "localize_log.json"

# ========= Utility Functions =========
def force_rotate(img_path: Path):
    try:
        img = Image.open(img_path)
        img = img.rotate(-90, expand=True)
        img.save(img_path)
        print(f">>> Rotated image: {img_path.name}")
    except Exception as e:
        print(f">>> Rotate skipped: {e}")

def clean_startup_dirs():
    query_dir = HERE / "datasets" / "query"
    if query_dir.exists():
        shutil.rmtree(query_dir)
    query_dir.mkdir(parents=True, exist_ok=True)

def read_log():
    if not LOG_PATH.exists():
        return []
    try:
        return json.loads(LOG_PATH.read_text())
    except:
        return []

def write_log(history):
    LOG_PATH.write_text(json.dumps(history, indent=2))

# ========= Flask =========
app = Flask(__name__)
mapping_batch_started = False


# =========================
# REBUILD (Mapping)
# =========================
@app.route("/rebuild", methods=["POST"])
def rebuild_route():
    global mapping_batch_started

    if request.form.get("done") == "true":
        if SFM_DIR.exists():
            shutil.rmtree(SFM_DIR)
        if FEATURES.exists():
            FEATURES.unlink(missing_ok=True)
        if MATCHES.exists():
            MATCHES.unlink(missing_ok=True)

        try:
            sfm, feat, matches, refs = rebuild()
            mapping_batch_started = False

            return {
                "status": "ok",
                "num_refs": len(refs)
            }
        except Exception as e:
            return {"status": "error", "message": str(e)}, 500

    file = request.files.get("file")
    if not file:
        return {"status": "error", "message": "No file"}, 400

    if not mapping_batch_started:
        if DB_DIR.exists():
            shutil.rmtree(DB_DIR)
        DB_DIR.mkdir(parents=True, exist_ok=True)
        mapping_batch_started = True

    # Save uploaded mapping image
    img_path = DB_DIR / file.filename
    file.save(img_path)
    force_rotate(img_path)

    return {"status": "ok"}


# =========================
# LOCALIZE (Localization and Trajectory Logging)
# =========================
@app.route("/localize", methods=["POST"])
def localize_route():
    file = request.files.get("file")
    if not file:
        return {"status": "error", "message": "No file"}, 400

    # Save query image
    query_dir = HERE / "datasets" / "query"
    query_dir.mkdir(parents=True, exist_ok=True)
    img_path = query_dir / file.filename
    file.save(img_path)

    force_rotate(img_path)

    try:
        # Perform localization
        T_w_query, _ = localize_image(img_path)
        T_w_query = np.array(T_w_query)

        # Extract pose
        from scipy.spatial.transform import Rotation as R

        position = T_w_query[:3, 3].tolist()
        quat = R.from_matrix(T_w_query[:3, :3]).as_quat().tolist()

        # Save trajectory history
        arcore_pos_raw = request.form.get("arcore_position")
        arcore_quat_raw = request.form.get("arcore_quaternion")

        arcore_position = None
        arcore_quaternion = None

        try:
            if arcore_pos_raw:
                arcore_position = json.loads(arcore_pos_raw)
            if arcore_quat_raw:
                arcore_quaternion = json.loads(arcore_quat_raw)
        except Exception as e:
            print(">>> ARCore pose parse skipped:", e)

        record = {
            "timestamp": datetime.now().isoformat(),

            # HLOC localization result
            "hloc_position": position,
            "hloc_position_2d": [position[0], position[1]],
            "hloc_quaternion": quat,

            # Keep old field for compatibility
            "camera_position": [position[0], position[1]],
            "quaternion": quat,

            # ARCore pose from mobile client
            "arcore_position": arcore_position,
            "arcore_quaternion": arcore_quaternion
        }

        history = read_log()
        history.append(record)

        # Limit history length
        if len(history) > 200:
            history = history[-200:]

        write_log(history)

        return {
            "status": "ok",
            "position": position,
            "quaternion": quat,
            "path_count": len(history)
        }

    except Exception as e:
        print(">>> Localize ERROR:", e)
        return {"status": "error", "message": str(e)}, 500

    finally:
        if img_path.exists():
            img_path.unlink()


# =========================
# PATH (Trajectory Visualization)
# =========================
@app.route("/path", methods=["GET"])
def get_path():
    try:
        # Mapping trajectory
        mapping_points = []

        if SFM_DIR.exists():
            _, images, _ = read_model(SFM_DIR, ext=".bin")

            for img in images.values():
                q = img.qvec
                t = img.tvec

                w, x, y, z = q
                R = np.array([
                    [1-2*y*y-2*z*z, 2*x*y-2*z*w,   2*x*z+2*y*w],
                    [2*x*y+2*z*w,   1-2*x*x-2*z*z, 2*y*z-2*x*w],
                    [2*x*z-2*y*w,   2*y*z+2*x*w,   1-2*x*x-2*y*y]
                ])

                pos = -R.T @ t

                mapping_points.append({
                    "x": -pos[0],
                    "y":  pos[1]
                })

        # Localization history trajectory
        history = read_log()

        history_points = []
        for r in history:
            history_points.append({
                "x": -r["camera_position"][0],
                "y":  r["camera_position"][1]
            })

        # Current position
        current = history_points[-1] if history_points else None

        # Debug information
        print("\n====== DEBUG PATH ======")

        print(f"Mapping points: {len(mapping_points)}")
        if mapping_points:
            print("First 3 mapping:", mapping_points[:3])

        print(f"History points: {len(history_points)}")
        if history_points:
            print("Last 3 history:", history_points[-3:])

        print("Current:", current)

        print("========================\n")

        return jsonify({
            "mapping": mapping_points,
            "history": history_points,
            "current": current
        })

    except Exception as e:
        print(">>> PATH ERROR:", e)
        return jsonify({"error": str(e)}), 500


# =========================
# Server Startup
# =========================
if __name__ == "__main__":
    clean_startup_dirs()
    print(">>> DB_DIR =", DB_DIR)
    print(">>> OUTPUTS =", OUTPUTS)
    app.run(host="0.0.0.0", port=5001, debug=True)
