#!/usr/bin/env bash
set -euo pipefail

if ! command -v sdkmanager >/dev/null 2>&1; then
  echo "sdkmanager not found in PATH" >&2
  exit 1
fi

sdkmanager --install \
  "platform-tools" \
  "build-tools;35.0.0" \
  "platforms;android-36" \
  "cmdline-tools;latest"
