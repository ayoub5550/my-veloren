#!/bin/bash
# Firebase Test Lab Game Loop smoke for the native Veloren APK (dev2).
#   FTL_DEVICE=r8q FTL_VERSION=33 FTL_TIMEOUT=10m tools/ftl.sh native/out/my-veloren-dev2.apk dev2-r8q
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
  --device "model=$MODEL,version=$VER,locale=en,orientation=landscape" --timeout "${FTL_TIMEOUT:-10m}" \
  --results-history-name my-veloren --format=json >"$OUT/result.json" 2>"$OUT/run.err"
echo $? > "$OUT/gcloud.exit"
cat "$OUT/result.json"
B=$(grep -o 'storage/browser/[^] ]*' "$OUT/run.err" | head -1 | sed 's#^storage/browser/#gs://#; s#/$##')
if [ -n "$B" ]; then echo "$B" > "$OUT/bucket.txt"; "$(dirname "$(command -v "$GCLOUD")")/gsutil" -m cp -r "$B/*" "$OUT/" >/dev/null 2>&1 || true; fi
grep -ah " veloren" "$OUT"/*/logcat 2>/dev/null | grep -E "android:|PANIC|ERROR|Assets found|Started game" | head -60
echo "results: $OUT"
