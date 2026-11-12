#!/bin/bash
set -e
#
# Shard 6/6 — Credit Note warning bottom sheet tests.
#
# Four classes, and they fail for different reasons — check which one is red first.
#
#   CreditNoteWarningTests            real backend, real document.
#   CreditNoteLocaleFontScaleTests    real backend, real document: the warning in German and at
#                                     200 % font size.
#   CreditNoteLandscapeDarkTests      real backend, real document: dark mode and landscape.
#   CreditNoteMockBackendTests        canned backend (UiTestMockBackend). No network, no
#                                     server flag. Covers the frontend-flag-off rows, the
#                                     credit-note-with-lineItems case that no fixture can express,
#                                     and the camera scan with the SDK flag off.
#
# The three real-backend classes perform a real upload + analysis round trip against the Gini
# API. Their warning tests use credit_note.png (androidTest assets — copied to the device by the
# tests themselves via ImageUploader), which the backend must classify as
# businessDocType = CreditNote; the negative tests use test_image.jpeg.
# Precondition for those classes only: the /configurations flag creditNoteHintEnabled must be
# enabled for the API client. If they are all red while CreditNoteMockBackendTests is green, that
# flag (or the classification) regressed rather than the sheet logic breaking — look at
# CreditNoteWarningTests#test3 first. The mock class does not depend on either.
#
# Expected skips, not failures:
#   CreditNoteLocaleFontScaleTests    all three tests below Android 13 (no per-app language);
#                                     the 200 % tests where the device refuses the font size.
#   CreditNoteLandscapeDarkTests      where the device does not let a test switch dark mode.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_creditnote.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-creditnote-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  CreditNoteWarningTests \
  CreditNoteMockBackendTests \
  CreditNoteLocaleFontScaleTests \
  CreditNoteLandscapeDarkTests
