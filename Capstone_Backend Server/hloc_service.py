from pathlib import Path
import numpy as np

from hloc import extract_features, match_features, pairs_from_exhaustive, reconstruction

HERE = Path(__file__).resolve().parent
OUTPUTS = HERE / "outputs"
OUTPUTS.mkdir(parents=True, exist_ok=True)

DB_DIR = HERE / "datasets" / "mapping"
DB_DIR.mkdir(parents=True, exist_ok=True)

SFM_DIR   = OUTPUTS / "sfm"
FEATURES  = OUTPUTS / "features.h5"
MATCHES   = OUTPUTS / "matches.h5"
SFM_PAIRS = OUTPUTS / "pairs-sfm.txt"   
LOC_PAIRS = OUTPUTS / "pairs-loc.txt"   

FEATURE_CONF = extract_features.confs["aliked-n16"]
MATCHER_CONF = match_features.confs["aliked+lightglue"]


def list_references():
    """Lists the references used for map creation (filename strings relative to DB_DIR)."""
    valid_exts = {".jpg", ".jpeg", ".png"}
    return [
        p.name for p in DB_DIR.iterdir()
        if p.is_file() and p.suffix.lower() in valid_exts
    ]


def rebuild():
    """
    Perform the following steps on the mapping image in DB_DIR:
    1) Feature extraction -> FEATURES
    2) Generate exhaustive pairs -> SFM_PAIRS
    3) Matching -> MATCHES
    4) Incremental reconstruction -> SFM_DIR
    Return (SFM_DIR, FEATURES, MATCHES, references)
    """
    refs = list_references()
    if len(refs) == 0:
        raise RuntimeError(f"No mapping images found in {DB_DIR}")

    extract_features.main(
        FEATURE_CONF,
        image_dir=DB_DIR,
        image_list=refs,
        feature_path=FEATURES,
        overwrite=False,
    )

    pairs_from_exhaustive.main(
        SFM_PAIRS,
        image_list=refs,
    )

    match_features.main(
        MATCHER_CONF,
        pairs=SFM_PAIRS,
        features=FEATURES,
        matches=MATCHES,
        overwrite=False,
    )

    reconstruction.main(
        SFM_DIR,
        DB_DIR,
        SFM_PAIRS,
        FEATURES,
        MATCHES,
        image_list=refs,
    )

    return SFM_DIR, FEATURES, MATCHES, refs
