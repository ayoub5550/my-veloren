#!/usr/bin/env python3
"""Batch Unity runner (repo root).
  python3 tools/build.py compile    # scripts compile + project configured
  python3 tools/build.py playtest   # Linux player + autopilot under xvfb -> Artifacts/playtest.txt, Artifacts/shots/*.png
  python3 tools/build.py android    # ARM64 APK -> game/Builds/my-veloren.apk (VEL_VERSION_CODE, default 10)
Env: UNITY_EDITOR (default /work/unity/run_unity.sh: Unity 2022.3.62f3 + Android SDK/NDK r23b + JDK11 wrapper).
SPDX-License-Identifier: GPL-3.0-or-later
"""
import os, re, subprocess, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
GAME, ART = ROOT / "game", ROOT / "Artifacts"
UNITY = os.environ.get("UNITY_EDITOR", "/work/unity/run_unity.sh")


def unity(method, log, target=None, timeout=7200):
    cmd = [UNITY, "-batchmode", "-nographics", "-quit", "-projectPath", str(GAME),
           "-executeMethod", "MyVeloren.EditorTools.VelBuild." + method, "-logFile", str(log)]
    if target:
        cmd[1:1] = ["-buildTarget", target]
    print("$", " ".join(cmd), flush=True)
    rc = subprocess.call(cmd, timeout=timeout)
    text = log.read_text(errors="replace") if log.exists() else ""
    errs = sorted(set(re.findall(r".*error CS\d+.*", text)))
    for line in errs[:30] + re.findall(r"\[VelBuild\].*", text):
        print(line)
    return 1 if errs else rc


def playtest():
    env = dict(os.environ, VEL_AUTOPILOT="1", VEL_RESULTS=str(ART / "playtest.txt"), VEL_SHOTS=str(ART / "shots"),
               VEL_TOUCHUI="1", HOME=str(ART / "home"), LP_NUM_THREADS="8",
               LD_LIBRARY_PATH="/work/unity/libs/usr/lib/x86_64-linux-gnu:" + os.environ.get("LD_LIBRARY_PATH", ""),
               LD_PRELOAD="/work/unity/shim/libschedfix.so")
    (ART / "home").mkdir(parents=True, exist_ok=True)
    cmd = ["xvfb-run", "-a", "-s", "-screen 0 1280x576x24", str(GAME / "Builds/linux/myveloren.x86_64"), "-force-glcore",
           "-screen-width", "1280", "-screen-height", "576", "-screen-fullscreen", "0", "-logFile", str(ART / "player.log")]
    print("$", " ".join(cmd), flush=True)
    try:
        subprocess.call(cmd, env=env, timeout=int(os.environ.get("VEL_PLAYER_TIMEOUT", "1200")))
    except subprocess.TimeoutExpired:
        print("player timed out")
    res = ART / "playtest.txt"
    text = res.read_text() if res.exists() else ""
    print(text or "no results; see Artifacts/player.log")
    return 0 if "result=pass" in text else 1


def main():
    task = sys.argv[1] if len(sys.argv) > 1 else "compile"
    ART.mkdir(exist_ok=True)
    if task == "compile":
        return unity("Compile", ART / "compile.log")
    if task == "playtest":
        return unity("BuildLinux", ART / "build-linux.log", "Linux64") or playtest()
    if task == "android":
        os.environ.setdefault("VEL_VERSION_CODE", "10")
        return unity("BuildAndroid", ART / "build-android.log", "Android")
    print(__doc__)
    return 2


if __name__ == "__main__":
    sys.exit(main())
