#!/usr/bin/env bash
set -euo pipefail

if ! command -v gradle >/dev/null 2>&1; then
  echo "ERROR: Gradle is required but was not found in PATH." >&2
  echo "Install Gradle or use Android Studio's bundled Gradle, then rerun ./build_android.sh." >&2
  exit 127
fi

cd "$(dirname "$0")/android"
gradle :app:assembleDebug
