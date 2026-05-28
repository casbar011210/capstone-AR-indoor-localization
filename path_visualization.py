import os
os.environ["KMP_DUPLICATE_LIB_OK"] = "TRUE"

import json
import shutil
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path
from hloc.utils.read_write_model import read_model

HERE = Path(__file__).resolve().parent
SFM_DIR = HERE / "outputs" / "sfm"
LOG_PATH = HERE / "localize_log.json"


def load_sfm_positions():
    _, images, _ = read_model(SFM_DIR, ext=".bin")
    positions = []

    for img in images.values():
        q = img.qvec
        t = img.tvec
        w, x, y, z = q

        R = np.array([
            [1 - 2*y*y - 2*z*z, 2*x*y - 2*z*w,     2*x*z + 2*y*w],
            [2*x*y + 2*z*w,     1 - 2*x*x - 2*z*z, 2*y*z - 2*x*w],
            [2*x*z - 2*y*w,     2*y*z + 2*x*w,     1 - 2*x*x - 2*y*y]
        ])

        positions.append(-R.T @ t)

    return np.array(positions)


def load_log():
    if not LOG_PATH.exists():
        return []
    try:
        return json.loads(LOG_PATH.read_text())
    except Exception:
        return []


def clear_all():
    if LOG_PATH.exists():
        LOG_PATH.unlink()

    if SFM_DIR.exists():
        shutil.rmtree(SFM_DIR)

    for f in [
        HERE / "outputs" / "features.h5",
        HERE / "outputs" / "matches.h5",
        HERE / "outputs" / "pairs-sfm.txt"
    ]:
        if f.exists():
            f.unlink()

    mapping_dir = HERE / "datasets" / "mapping"
    if mapping_dir.exists():
        shutil.rmtree(mapping_dir)
        mapping_dir.mkdir()

    print(">>> all cleared — close window and rebuild")


def draw(ax, sfm_pos, records):
    ax.clear()
    ax.set_facecolor("#1e1e2e")

    # 上下 + 左右都反转
    xs = sfm_pos[:, 0]
    ys = -sfm_pos[:, 1]

    ax.scatter(xs, ys, c="#555577", s=25, zorder=2, label="Mapping refs")

    if not records:
        ax.set_title("No data yet — run /localize first", color="#ff6b6b", fontsize=12)
    else:
        hx = [r["camera_position"][0] for r in records[:-1]]
        hy = [-r["camera_position"][1] for r in records[:-1]]

        if hx:
            ax.scatter(hx, hy, c="#ff6b6b", s=30, alpha=0.35, zorder=4, label="History")
            ax.plot(
                hx + [records[-1]["camera_position"][0]],
                hy + [-records[-1]["camera_position"][1]],
                c="#ff6b6b",
                lw=0.8,
                alpha=0.3
            )

        px = records[-1]["camera_position"][0]
        py = -records[-1]["camera_position"][1]

        ax.scatter(px, py, c="#ff3366", s=220, zorder=6, label="Current position")

        qx, qy, qz, qw = records[-1]["quaternion"]

        fwd_x = 2 * (qx*qz + qw*qy)
        fwd_y = -2 * (qy*qz - qw*qx)

        arrow_len = max(xs.ptp(), ys.ptp()) * 0.1

        ax.annotate(
            "",
            xy=(px + fwd_x * arrow_len, py + fwd_y * arrow_len),
            xytext=(px, py),
            arrowprops=dict(arrowstyle="->", color="#ff3366", lw=2)
        )

        ax.set_title(
            f"Top-down view (XY)  |  pos: ({px:.3f}, {py:.3f})  |  records: {len(records)}",
            color="white",
            fontsize=12
        )

    margin = max(xs.ptp(), ys.ptp()) * 0.5

    ax.set_xlim(xs.min() - margin, xs.max() + margin)
    ax.set_ylim(ys.min() - margin, ys.max() + margin)

    ax.set_xlabel("X", color="gray")
    ax.set_ylabel("Y (flipped)", color="gray")
    ax.set_aspect("equal")
    ax.tick_params(colors="gray")

    for spine in ax.spines.values():
        spine.set_edgecolor("#444")

    ax.legend(facecolor="#2a2a3e", labelcolor="white", fontsize=9)
    ax.grid(True, color="#333344", linewidth=0.5)


def main():
    sfm_pos = load_sfm_positions()

    fig, ax = plt.subplots(figsize=(8, 8))
    fig.patch.set_facecolor("#1e1e2e")

    draw(ax, sfm_pos, load_log())
    plt.tight_layout()

    def on_key(event):
        if event.key == "r":
            draw(ax, sfm_pos, load_log())
            fig.canvas.draw()
            print(">>> refreshed")
        elif event.key == "c":
            clear_all()
            plt.close()

    fig.canvas.mpl_connect("key_press_event", on_key)

    print(">>> 'r' to refresh  |  'c' to clear all and close")
    plt.show()


if __name__ == "__main__":
    main()