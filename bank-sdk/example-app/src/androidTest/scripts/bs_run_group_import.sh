#!/bin/bash
set -e
#
# Shard 4/4 — Import (pdf/image) / file-import errors / error screens / open-with.
#
# Note: ErrorScreenTests#test2_verifyNetworkErrorScreen auto-skips on BrowserStack
# (the device network can't be disabled there); it still runs on local devices.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_import.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-import-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  ImportPdfImageTests \
  FileImportErrorDialogTests \
  ErrorScreenTests \
  OpenWithTest
