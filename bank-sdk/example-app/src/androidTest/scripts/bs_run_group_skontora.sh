#!/bin/bash
set -e
#
# Skonto + Return Assistant tests (PP-3916): the Xray folder "QAlity imported test cases /
# Skonto + RA" (PP-2807 … PP-2815). Two classes, and they fail for different reasons — check
# which one is red first.
#
#   SkontoReturnAssistantTests              5 tests, REAL backend: the Skonto row and footer
#                                           (PP-2807), switching it off (PP-2808), the Skonto
#                                           screen opened from RA (PP-2809), and the Skonto / RA
#                                           configuration switches in portrait and landscape
#                                           (PP-2814, PP-2815). Fixture ra_skonto_valid_2028.pdf:
#                                           the OTTO invoice (two line items, 3 % Skonto) dated
#                                           2028 so the discount stays claimable — the API drops
#                                           an expired Skonto. A failure that names
#                                           missing lineItems or skontoDiscounts is the API, not
#                                           the SDK.
#   SkontoReturnAssistantMockBackendTests   4 tests, canned backend (UiTestMockBackend): the date
#                                           edge cases cash only, expires today, expired and cash
#                                           today (PP-2810 … PP-2813). The mock computes the due
#                                           date from today, which no committed document can.
#                                           They skip themselves within 30 minutes of midnight.
#
# The PDF travels in the test APK and is copied into Downloads by the test, like skonto_past.pdf.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_skontora.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-skontora-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  SkontoReturnAssistantTests \
  SkontoReturnAssistantMockBackendTests
