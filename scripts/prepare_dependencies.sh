#!/usr/bin/env bash
set -euo pipefail

PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ASSETS_DIR="$PROJECT_DIR/app/src/main/assets"
JNI_DIR="$PROJECT_DIR/app/src/main/jniLibs/arm64-v8a"
CPP_DIR="$PROJECT_DIR/app/src/main/cpp"
WORK_DIR="$(mktemp -d)"

cleanup() {
  rm -rf "$WORK_DIR"
}
trap cleanup EXIT

mkdir -p "$ASSETS_DIR" "$JNI_DIR" "$CPP_DIR"

if [[ ! -f "$CPP_DIR/libmp3lame/lame.c" ]]; then
  git clone --depth 1 https://github.com/naman14/TAndroidLame.git "$WORK_DIR/tandroidlame"
  rm -rf "$CPP_DIR/libmp3lame"
  cp -R "$WORK_DIR/tandroidlame/androidlame/src/main/jni/libmp3lame" "$CPP_DIR/libmp3lame"
fi

SHERPA_VERSION="v1.13.5"
SHERPA_ARCHIVE="sherpa-onnx-${SHERPA_VERSION}-android.tar.bz2"
curl --fail --location --retry 4 \
  "https://github.com/k2-fsa/sherpa-onnx/releases/download/${SHERPA_VERSION}/${SHERPA_ARCHIVE}" \
  --output "$WORK_DIR/$SHERPA_ARCHIVE"
mkdir -p "$WORK_DIR/sherpa-libs"
tar -xf "$WORK_DIR/$SHERPA_ARCHIVE" -C "$WORK_DIR/sherpa-libs"

for library in libonnxruntime.so libsherpa-onnx-jni.so; do
  source_path="$(find "$WORK_DIR/sherpa-libs" -type f -path '*arm64-v8a*' -name "$library" | head -n 1)"
  if [[ -z "$source_path" ]]; then
    echo "No se encontró $library para arm64-v8a" >&2
    exit 1
  fi
  cp "$source_path" "$JNI_DIR/$library"
done

download_model() {
  local archive="$1"
  local extracted="$2"
  local destination="$3"
  if [[ -d "$ASSETS_DIR/$destination" ]]; then
    return
  fi
  curl --fail --location --retry 4 \
    "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/$archive" \
    --output "$WORK_DIR/$archive"
  tar -xf "$WORK_DIR/$archive" -C "$WORK_DIR"
  rm -rf "$WORK_DIR/$extracted/test_wavs"
  find "$WORK_DIR/$extracted" -maxdepth 1 -type f \
    \( -name '*.wav' -o -name '*.md' -o -name '*.sh' \) -delete
  mv "$WORK_DIR/$extracted" "$ASSETS_DIR/$destination"
}

download_model \
  "sherpa-onnx-pocket-tts-int8-2026-01-26.tar.bz2" \
  "sherpa-onnx-pocket-tts-int8-2026-01-26" \
  "pocket"

download_model \
  "sherpa-onnx-supertonic-3-tts-int8-2026-05-11.tar.bz2" \
  "sherpa-onnx-supertonic-3-tts-int8-2026-05-11" \
  "supertonic"

test -f "$ASSETS_DIR/pocket/lm_flow.int8.onnx"
test -f "$ASSETS_DIR/supertonic/vector_estimator.int8.onnx"
test -f "$JNI_DIR/libsherpa-onnx-jni.so"

du -sh "$ASSETS_DIR" "$JNI_DIR" "$CPP_DIR/libmp3lame"
