#!/bin/bash
set -e
#
# Shard 2/4 — Digital-invoice tests (network extraction of line items).
#
# These are isolated in their own build on purpose: they need the digital-invoice
# onboarding/flow to trigger cleanly, which is unreliable when run in one giant
# invocation alongside the other ~130 tests. On their own they pass reliably.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_digitalinvoice.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-digitalinvoice-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  DigitalInvoiceScreenTests \
  DigitalInvoiceEditButtonTests
