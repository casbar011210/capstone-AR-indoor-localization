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
    """列出用于建图的 reference 名单（相对 DB_DIR 的文件名字符串）。"""
    valid_exts = {".jpg", ".jpeg", ".png"}
    return [
        p.name for p in DB_DIR.iterdir()
        if p.is_file() and p.suffix.lower() in valid_exts
    ]


def rebuild():
    """
    对 DB_DIR 里的 mapping 图像进行：
      1) 提特征 -> FEATURES
      2) 生成 pairs（exhaustive）-> SFM_PAIRS
      3) 匹配 -> MATCHES
      4) 增量重建 -> SFM_DIR
    返回 (SFM_DIR, FEATURES, MATCHES, references)
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
