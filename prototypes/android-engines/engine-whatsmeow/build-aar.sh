#!/usr/bin/env bash
# Compila el módulo Go (whatsmeow) a app/libs/wacheck.aar para arm64-v8a.
# Requiere Go, gomobile y el Android NDK (ver specs/2026-09-23-comparar-motores-android).
set -euo pipefail
cd "$(dirname "$0")/go"

: "${ANDROID_HOME:=${LOCALAPPDATA:-$HOME}/Android/Sdk}"
if [[ -z "${ANDROID_NDK_HOME:-}" ]]; then
  ANDROID_NDK_HOME="$(ls -d "$ANDROID_HOME"/ndk/* | sort -V | tail -1)"
fi
export ANDROID_HOME ANDROID_NDK_HOME

go test ./...
mkdir -p ../../app/libs
# -s -w: sin símbolos de depuración (libgojni.so pasa de ~35 MB a ~17 MB)
gomobile bind -target=android/arm64 -androidapi 29 -javapkg com.whatscheck.gen \
  -trimpath -ldflags="-s -w" -o ../../app/libs/wacheck.aar .
ls -la ../../app/libs/wacheck.aar
