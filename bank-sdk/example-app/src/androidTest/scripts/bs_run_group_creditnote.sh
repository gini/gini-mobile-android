#!/bin/bash
set -e
#
# Shard 6/6 — Credit Note warning bottom sheet tests.
#
# Every test performs a real upload + analysis round trip against the Gini API. The
# warning tests use credit_note.png (androidTest assets — copied to the device by the
# tests themselves via ImageUploader), which the backend must classify as
# businessDocType = CreditNote. The two negative tests use test_image.jpeg.
# Preconditions: the /configurations flag creditNoteHintEnabled must be enabled for the
# API client. An all-red shard almost always means that flag regressed rather than the
# sheet logic breaking.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_creditnote.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-creditnote-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  CreditNoteWarningTests
