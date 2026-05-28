import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

from pathlib import Path
import time
import shutil
import numpy as np
import pycolmap

from hloc import extract_features, match_features, pairs_from_exhaustive
from hloc.localize_sfm import QueryLocalizer, pose_from_cluster
from hloc.utils.read_write_model import read_model

from hloc_service import OUTPUTS, DB_DIR

SFM_DIR  = OUTPUTS / "sfm"
FEATURES = OUTPUTS / "features.h5"
MATCHES  = OUTPUTS / "matches.h5"


def qvec_to_rotmat(qvec):
    w, x, y, z = qvec

    return np.array([
        [1-2*y*y-2*z*z, 2*x*y-2*z*w,   2*x*z+2*y*w],
        [2*x*y+2*z*w,   1-2*x*x-2*z*z, 2*y*z-2*x*w],
        [2*x*z-2*y*w,   2*y*z+2*x*w,   1-2*x*x-2*y*y]
    ])


def localize_image_return_Tw(img_path: Path):

    img_path = Path(img_path)
    assert img_path.exists(), f"Query image not found: {img_path}"

    # Create temporary localization job directory
    queries_dir = OUTPUTS / "queries"
    queries_dir.mkdir(parents=True, exist_ok=True)

    job_dir = queries_dir / f"job_{int(time.time())}"
    job_dir.mkdir(parents=True, exist_ok=True)

    tmp_img = job_dir / img_path.name
    shutil.copy2(img_path, tmp_img)

    # Step 1: Extract query image features
    extract_features.main(
        extract_features.confs["aliked-n16"],
        image_dir=job_dir,
        image_list=[tmp_img.name],
        feature_path=FEATURES,
        overwrite=True
    )

    # Step 2: Generate localization image pairs
    valid_exts = {".jpg", ".jpeg", ".png"}

    references = [
        p.name for p in DB_DIR.iterdir()
        if p.is_file() and p.suffix.lower() in valid_exts
    ]

    pairs_path = job_dir / "pairs-loc.txt"

    pairs_from_exhaustive.main(
        pairs_path,
        image_list=[tmp_img.name],
        ref_list=references
    )

    # Step 3: Match query image features with reference images
    match_features.main(
        match_features.confs["aliked+lightglue"],
        pairs=pairs_path,
        features=FEATURES,
        matches=MATCHES,
        overwrite=True
    )

    # Step 4: Perform visual localization
    _, images, _ = read_model(SFM_DIR, ext=".bin")

    model = pycolmap.Reconstruction(SFM_DIR)

    camera = pycolmap.infer_camera_from_image(tmp_img)

    ref_ids = [
        model.find_image_with_name(r).image_id
        for r in references
    ]

    conf = {
        "estimation": {
            "ransac": {
                "max_error": 12
            }
        },
        "refinement": {
            "refine_focal_length": True,
            "refine_extra_params": True
        },
    }

    localizer = QueryLocalizer(model, conf)

    ret, log = pose_from_cluster(
        localizer,
        tmp_img.name,
        camera,
        ref_ids,
        FEATURES,
        MATCHES
    )

    print("num_inliers:", ret.get("num_inliers", "N/A"))
    print("localization log:", log)

    assert "cam_from_world" in ret, f"Localization failed: {log}"

    # Step 5: Convert query pose to world coordinates
    T_cw = np.eye(4)
    T_cw[:3, :4] = ret["cam_from_world"].matrix()

    T_w_query = np.linalg.inv(T_cw)

    # Step 6: Retrieve reference image pose
    best_ref = ret.get("best_cluster", None)

    if best_ref is None:
        ref_name = pairs_path.read_text().strip().splitlines()[0].split()[1]
    else:
        ref_name = best_ref[0] if isinstance(best_ref, list) else best_ref

    img = next(i for i in images.values() if i.name == ref_name)

    q = img.qvec
    t = img.tvec

    R = qvec_to_rotmat(q)

    T_w_ref = np.eye(4)
    T_w_ref[:3, :3] = R.T
    T_w_ref[:3, 3] = -R.T @ t

    # Convert matrices to JSON-compatible lists
    T_w_query = [
        [float(x) for x in row]
        for row in T_w_query
    ]

    T_w_ref = [
        [float(x) for x in row]
        for row in T_w_ref
    ]

    return T_w_query, T_w_ref


def localize_image(img_path: Path):
    return localize_image_return_Tw(img_path)
