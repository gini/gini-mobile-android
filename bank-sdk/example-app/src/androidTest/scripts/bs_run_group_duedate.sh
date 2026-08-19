#!/bin/bash
set -e
#
# Shard 5/5 — Due Date Hint / Schedule Payment bottom sheet tests (PP-3301).
#
# Every test performs a real upload + analysis round trip against the Gini API. Most
# tests use the validated fixtures invoice_future_due.jpeg / invoice_no_due_date.jpeg
# (androidTest assets — copied to the device by the tests themselves). The Return
# Assistant test additionally drives Testrechnung-RA-1.pdf through DocumentsUI and
# relies on the pre-loaded UploadPDF media, which bs_build_and_upload.sh uploads
# unconditionally for every shard.
# Preconditions: the /configurations flags paymentDueHintEnabled and
# paymentScheduleHintEnabled must be enabled for the API client (PP-3260).
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_duedate.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-duedate-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  DueDateHintBottomSheetTests \
  SchedulePaymentBottomSheetTests
