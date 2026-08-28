#!/bin/bash
set -e
#
# Runs the shards in the CURRENT RELEASE's test scope as separate BrowserStack builds,
# building the APKs and uploading them (and the media) only ONCE — the same trick
# bs_run_all_groups.sh uses.
#
# All builds land in the release project set in bs_build_and_upload.sh (BS_PROJECT),
# so the whole release sign-off sits in one place in the App Automate dashboard.
#
# Scope for 4.5.0 — the features this release ships or changes:
#   duedate     – Due Date Hint / Schedule Payment bottom sheets
#   creditnote  – Credit Note warning bottom sheet
#   import      – file upload: pdf/image import, file-import errors, open-with
#
# ── REUSING THIS SCRIPT FOR THE NEXT RELEASE ────────────────────────────────────
# Two edits, both by hand — the scope of a release is a decision, not something a
# script can work out:
#
#   1. The release project. Bump BS_PROJECT in bs_build_and_upload.sh to the new
#      version, e.g. GiniBankSDK-Android-4.6.0. That one line renames the container
#      for every script in this directory, not just this one.
#
#   2. The scope. Edit the run_group calls in the RELEASE SCOPE block below — add,
#      remove or change shards to match what the release actually touches, and update
#      the "Scope for <version>" list above so the header stays honest.
#
# The test class names to use are the same ones bs_run_all_groups.sh lists; a shard
# here can hold any subset of them. To run the ENTIRE suite instead of a scope, use
# bs_run_all_groups.sh — leave this script for release sign-off.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_release.sh
#
# Override the release project for an RC:
#   BS_PROJECT="GiniBankSDK-Android-4.5.0-RC1" ./bs_run_release.sh
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

# ── Step B: trigger each in-scope shard, reusing the uploaded artifacts ──────────
run_group() {
  local name="$1"; shift
  echo ""
  echo "=== Triggering shard: $name ==="
  BUILD_NAME="release-${name}-${STAMP}" "$SCRIPT_DIR/bs_build_and_upload.sh" "$@"
}

# ══ RELEASE SCOPE ═══════════════════════════════════════════════════════════════
#
# One run_group call = one BrowserStack build. Edit this block for each release.
#
#   run_group "<shard-name>" <TestClass> [<TestClass> ...]
#
#   <shard-name>  becomes the build name in the dashboard: release-<shard-name>-<stamp>.
#                 Keep it short and lowercase — it is what you scan the build list for.
#   <TestClass>   short class name from ui/testcases/ (the package is prepended for
#                 you). A single method also works: MyTests#test3_someCase
#
# TO ADD a feature to the release scope
#   Add one line. Give it its own shard unless it is genuinely part of an existing
#   one — separate shards run in parallel and fail independently, so a red build
#   points straight at the feature:
#
#     run_group "skonto" \
#       SkontoBottomSheetTests
#
# TO REMOVE a feature from the release scope
#   Delete its run_group call — do NOT delete the test class or its
#   bs_run_group_<feature>.sh wrapper. Out of scope for this release only means
#   "not part of this sign-off"; the test still runs in bs_run_all_groups.sh.
#
# TO CHANGE what a shard covers
#   Add or remove class names on the existing call. Any number is fine, but keep a
#   shard under roughly 10 minutes of device time or feedback gets slow.
#
# AFTER EDITING
#   1. Update the "Scope for <version>" list in the header above.
#   2. Bump BS_PROJECT in bs_build_and_upload.sh if this is a new release.
#   3. Run `bash -n bs_run_release.sh` to catch a missing backslash.
#
# ════════════════════════════════════════════════════════════════════════════════
run_group "duedate" \
  DueDateHintBottomSheetTests \
  SchedulePaymentBottomSheetTests

run_group "creditnote" \
  CreditNoteWarningTests

# File upload. ErrorScreenTests#test2_verifyNetworkErrorScreen auto-skips on
# BrowserStack (the device network can't be disabled there); it still runs locally.
run_group "import" \
  ImportPdfImageTests \
  FileImportErrorDialogTests \
  ErrorScreenTests \
  OpenWithTest

# ══ END RELEASE SCOPE ═══════════════════════════════════════════════════════════

echo ""
echo "=== Release scope triggered. Check the BrowserStack dashboard. ==="
