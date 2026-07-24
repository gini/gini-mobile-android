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

# Load the artifact URLs for reuse. Parse them explicitly rather than `source`-ing the
# file, so its contents are never executed as shell code.
read_url() { grep -m1 "^$1=" "$URLS_FILE" | cut -d= -f2-; }
APP_URL="$(read_url APP_URL)"
TEST_URL="$(read_url TEST_URL)"
IMAGE_URL="$(read_url IMAGE_URL)"
PDF_URL="$(read_url PDF_URL)"
SAMPLE_PDF_URL="$(read_url SAMPLE_PDF_URL)"
export APP_URL TEST_URL IMAGE_URL PDF_URL SAMPLE_PDF_URL

if [ -z "$APP_URL" ] || [ -z "$TEST_URL" ]; then
  echo "Error: could not read artifact URLs from $URLS_FILE"; exit 1
fi

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
