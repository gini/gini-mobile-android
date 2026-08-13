#!/bin/bash
set -e
#
# Shard 1/4 — UI / navigation tests (fast, stable, no network extraction).
#
# The four bs_run_group_*.sh scripts together cover ALL 14 UI test classes with no
# overlap, so running all four = the full suite, but split into small isolated builds
# that are fast and reliable (see the note in each script).
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_ui.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-ui-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  CaptureScreenTests \
  OnboardingScreenTests \
  MainScreenTests \
  HelpScreenTests
