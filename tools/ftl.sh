#!/bin/bash
# Firebase Test Lab Game Loop run for the native Veloren APK (dev2+).
#   FTL_DEVICE=r8q FTL_VERSION=33 FTL_TIMEOUT=15m FTL_SCENARIOS=1,2 tools/ftl.sh native/out/my-veloren-dev3.apk dev3-r8q
# Scenarios: 1 = touch-injected play + all dev.3 checks, 2 = relaunch, layout restored + reset,
# 3 = dev.4 graphics-tier benchmark (desktop/high/medium/low, 40 s each) + 10-min soak (use FTL_TIMEOUT=35m).
# 4 = dev.5 background/foreground x5 (cover activity) + autosave + SIGKILL, 5 = relaunch after the kill:
#     save kept, assets not re-extracted, crash report collected, Back opens/closes the Esc menu (run 4,5 together).
# 6 = dev.5 (5.6) terrain occlusion culling A/B: same camera sweep with culling off/on (ABBA), 30 s each.
# 7 = dev.6 touch UI: UI scale, chat via the Android text dialog (Arabic), bag long-press/double-tap,
#     two-finger scroll (crafting), map pinch zoom + long-press marker.
# 8 = dev.6 character creation form with the name from the text dialog (run FIRST on a fresh install: 8,7,1).
# 9 = dev.7 phone fixes: layout fits without overlap, ability buttons follow the hotbar, AThermal API + cool-down steps,
#     floating stick (full/follow/stop), Glide shown in Jump's place while airborne. Run 9,1,2.
# 10 = dev.8 content audit (towns, NPC talk, farm harvest, caves, 5 dungeon kinds + bosses, day/night, rain, mining,
#      hunting, collar taming, riding, gliding, boat/airship). Uses /buff invulnerability + /site teleports. FTL_TIMEOUT=60m.
# 11 = dev.8 worlds/characters/saves: add the ready-made 'Small land', export (tar → Download) + import, second character.
#      Run 11,10,1 on a fresh install.
# 12 = dev.9 gamepad (sticks, triggers, A/B/Start, d-pad), Bluetooth keyboard (W, Space, I) and mouse (look, click,
#      wheel) through the window's input pipeline; touch overlay hides / comes back.
# 13 = dev.9 multiplayer on the official server. FTL_MP_ACCOUNT=<file with username, password[, server] lines>
#      pushes the test account to the app's external files folder (never commit it). Without it the run logs in with
#      a non-existent account and passes when the server + auth server answer. Run 12,13,1.
# Spark quota: ~10 virtual-device tests/day as well; a rejected matrix ends with TEST_QUOTA_EXCEEDED.
# Results: grep 'VEL-CHECK\|VEL-SCENARIO' in <OUT>/<device>/logcat.
# Project ayoub-261d7 (Spark: ~5 physical tests/day). Never enable billing; never cancel a running matrix.
# Needs a service-account key (FIREBASE_SA_JSON, never committed). Run as a background job (~15 min with upload).
set -u
APK="${1:?apk}"; TAG="${2:?tag}"
GCLOUD="${GCLOUD:-gcloud}"; PROJECT="${FTL_PROJECT:-ayoub-261d7}"
MODEL="${FTL_DEVICE:-r8q}"; VER="${FTL_VERSION:-33}"
OUT="${OUT:-Artifacts/ftl/$TAG/$(date -u +%Y%m%d-%H%M%S)}"; mkdir -p "$OUT"
sha256sum "$APK" > "$OUT/apk.sha256"; wc -c < "$APK" > "$OUT/apk.bytes"
[ -n "${FIREBASE_SA_JSON:-}" ] && "$GCLOUD" auth activate-service-account --key-file="$FIREBASE_SA_JSON" >/dev/null 2>&1
"$GCLOUD" firebase test android run --project "$PROJECT" --type game-loop --app "$APK" \
  --device "model=$MODEL,version=$VER,locale=en,orientation=landscape" --timeout "${FTL_TIMEOUT:-10m}" ${FTL_SCENARIOS:+--scenario-numbers "$FTL_SCENARIOS"} \
  ${FTL_MP_ACCOUNT:+--other-files "/sdcard/Android/data/com.ayoub.myveloren/files/mp_account.txt=$FTL_MP_ACCOUNT"} \
  --results-history-name my-veloren --format=json >"$OUT/result.json" 2>"$OUT/run.err"
echo $? > "$OUT/gcloud.exit"
cat "$OUT/result.json"
B=$(grep -o 'storage/browser/[^] ]*' "$OUT/run.err" | head -1 | sed 's#^storage/browser/#gs://#; s#/$##')
if [ -n "$B" ]; then echo "$B" > "$OUT/bucket.txt"; "$(dirname "$(command -v "$GCLOUD")")/gsutil" -m cp -r "$B/*" "$OUT/" >/dev/null 2>&1 || true; fi
grep -ah " veloren" "$OUT"/*/logcat 2>/dev/null | grep -E "android:|PANIC|VEL-CHECK|VEL-SCENARIO|jni:" | head -80
echo "results: $OUT"
