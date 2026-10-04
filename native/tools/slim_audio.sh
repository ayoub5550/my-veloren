#!/bin/bash
# dev.10 "lighter": re-encode the heaviest music files for the APK.
#
#   native/tools/slim_audio.sh <veloren-src> <out-dir>
#
# Every .ogg under assets/voxygen/audio whose bit rate is above 150 kb/s or whose
# sample rate is above 48 kHz is re-encoded to Ogg Vorbis q4 (~128 kb/s VBR) at
# <= 48 kHz (phones output 48 kHz; 96/192 kHz files only cost decode CPU).
# Nothing is removed: same files, same paths, same format (Vorbis, which
# Veloren's decoder reads). Output goes to <out-dir>/assets/... for tar to
# append; a list of the replaced paths is written to <out-dir>/replaced.txt.
# Results are cached by the source file's sha256 in $SLIM_CACHE.
set -euo pipefail
SRC=$1; OUT=$2
CACHE=${SLIM_CACHE:-$OUT/../audio-slim-cache}
mkdir -p "$OUT" "$CACHE"; : > "$OUT/replaced.txt"
cd "$SRC"
find assets/voxygen/audio -name '*.ogg' -size +300k | sort | while read -r f; do
  read -r sr br < <(ffprobe -v error </dev/null -select_streams a:0 -show_entries stream=sample_rate:format=bit_rate \
      -of default=nw=1:nk=1 "$f" | tr '\n' ' '; echo)
  if [ "${br:-0}" -le 150000 ] && [ "${sr:-0}" -le 48000 ]; then continue; fi
  h=$(sha256sum "$f" | cut -c1-32); c="$CACHE/$h.ogg"
  if [ ! -s "$c" ]; then
    rate=$(( sr > 48000 ? 48000 : sr ))
    ffmpeg -nostdin -v error -y -i "$f" -map 0:a:0 -map_metadata 0 -c:a libvorbis -q:a 4 -ar "$rate" "$c.tmp.ogg"
    mv "$c.tmp.ogg" "$c"
  fi
  mkdir -p "$OUT/$(dirname "$f")"; cp "$c" "$OUT/$f"
  echo "$f" >> "$OUT/replaced.txt"
  echo "slim: $f ${sr}Hz $((br/1000))k $(stat -c%s "$f") -> $(stat -c%s "$c")"
done
n=$(wc -l < "$OUT/replaced.txt")
before=$(cd "$SRC" && xargs -a "$OUT/replaced.txt" stat -c%s | awk '{s+=$1} END{print s+0}')
after=$(cd "$OUT" && xargs -a "$OUT/replaced.txt" stat -c%s | awk '{s+=$1} END{print s+0}')
echo "slim: $n files, $before -> $after bytes"
