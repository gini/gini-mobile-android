#!/bin/bash
set -e
#
# Shard 6/6 — Credit Note warning bottom sheet tests.
#
# Three classes, and they fail for different reasons — check which one is red first.
#
#   CreditNoteWarningTests            real backend, real document.
#   CreditNoteMockBackendTests        canned backend (UiTestMockBackend). No network, no
#                                     server flag. Covers the frontend-flag-off rows and the
#                                     credit-note-with-lineItems case that no fixture can express.
# CreditNoteWarningTests performs a real upload + analysis round trip against the Gini API.
# Its warning tests use credit_note.png (androidTest assets — copied to the device by the
# tests themselves via ImageUploader), which the backend must classify as
# businessDocType = CreditNote; the negative tests use test_image.jpeg.
# Precondition for that class only: the /configurations flag creditNoteHintEnabled must be
# enabled for the API client. If CreditNoteWarningTests is all red while the other two are
# green, that flag regressed rather than the sheet logic breaking — the mock classes do not
# depend on it.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_creditnote.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-creditnote-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  CreditNoteWarningTests \
  CreditNoteMockBackendTests
