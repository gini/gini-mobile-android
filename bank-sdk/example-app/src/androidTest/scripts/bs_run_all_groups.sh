#!/bin/bash
set -e
#
# Runs the ENTIRE UI suite as 4 sharded BrowserStack builds — but builds the APKs and
# uploads them (and the media) only ONCE, then triggers all four builds reusing those
# artifacts. Much faster than running the four group scripts separately (which would
# rebuild + re-upload each time).
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_all_groups.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
STAMP="$(date +%Y%m%d-%H%M%S)"

# ── Step A: build + upload once (no test run) ────────────────────────────────────
URLS_FILE="$(mktemp -t bs_artifacts.XXXXXX)"
trap 'rm -f "$URLS_FILE"' EXIT

echo "=== Building APKs and uploading to BrowserStack (once) ==="
SKIP_TRIGGER=true ARTIFACT_URLS_FILE="$URLS_FILE" "$SCRIPT_DIR/bs_build_and_upload.sh"

# Load APP_URL / TEST_URL / IMAGE_URL / PDF_URL / SAMPLE_PDF_URL for reuse
# shellcheck disable=SC1090
source "$URLS_FILE"
export APP_URL TEST_URL IMAGE_URL PDF_URL SAMPLE_PDF_URL

# ── Step B: trigger each shard, reusing the uploaded artifacts ───────────────────
# Group name -> class list. Together these cover all 14 UI test classes, no overlap.
run_group() {
  local name="$1"; shift
  echo ""
  echo "=== Triggering shard: $name ==="
  BUILD_NAME="all-${name}-${STAMP}" "$SCRIPT_DIR/bs_build_and_upload.sh" "$@"
}

run_group "ui" \
  CaptureScreenTests OnboardingScreenTests MainScreenTests HelpScreenTests

run_group "digitalinvoice" \
  DigitalInvoiceScreenTests DigitalInvoiceEditButtonTests

run_group "extraction" \
  ExtractionScreenTests ReviewScreenTests NoResultsTests ProductTagConfigurationTests

run_group "import" \
  ImportPdfImageTests FileImportErrorDialogTests ErrorScreenTests OpenWithTest

echo ""
echo "=== All 4 shards triggered. Check the BrowserStack dashboard for the 4 builds. ==="
