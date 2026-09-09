#!/bin/bash
set -e
#
# Runs the ENTIRE UI suite as sharded BrowserStack builds — but builds the APKs and
# uploads them (and the media) only ONCE, then triggers every build reusing those
# artifacts. Much faster than running the group scripts separately (which would
# rebuild + re-upload each time).
#
# The shard list below is the whole suite, with one deliberate exception: every test class
# appears in exactly one shard EXCEPT DigitalInvoiceSkontoTests, which is excluded on purpose
# (see the note further down, and bs_run_group_smoke.sh's header for why). When you add a test
# class, add it to a shard here too, or `all_groups` silently stops meaning "everything".
# Comparing `ls ../java/net/gini/android/bank/sdk/exampleapp/ui/testcases/` against the
# run_group calls below is the check — expect exactly that one class, plus the abstract bases
# (SmokeJourneyTestBase, WarningBottomSheetTestBase), to be absent.
#
# bs_run_group_smoke.sh is NOT one of these shards and is deliberately not triggered here:
# it is a curated cross-cutting selection (see ../COVERAGE.md) that overlaps several
# shards, so running it alongside them would execute those tests twice.
#
# PACING: each shard reserves one session per device, so this script asks for (shards x
# devices) parallel sessions at once — more than most plans allow. When the plan cannot
# supply them BrowserStack refuses the request outright — it
# does NOT hold it once its queue is full. bs_build_and_upload.sh therefore waits and retries
# the trigger (60s x 45 by default, BS_TRIGGER_WAIT / BS_TRIGGER_RETRIES to change). So a run
# that seems to hang between shards is usually waiting for a slot, and says so each minute.
# To avoid the wait entirely, run the shards yourself with the bs_run_group_<name>.sh scripts.
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
# Group name -> class list. Together these cover every UI test class, no overlap.
FAILED_SHARDS=()

SHARD_NAMES=()

run_group() {
  local name="$1"; shift
  SHARD_NAMES+=("$name")
  echo ""
  echo "=== Triggering shard: $name ==="
  # Run it as an `if` condition so errexit does not apply to it. Without this, `set -e`
  # aborts the whole script on the first shard that fails to trigger and every shard after
  # it is never attempted — so the last one in the list loses every time, silently.
  if BUILD_NAME="all-${name}-${STAMP}" "$SCRIPT_DIR/bs_build_and_upload.sh" "$@"; then
    return 0
  fi
  echo "!!! Shard '$name' was NOT triggered — see the error above. Continuing with the rest."
  FAILED_SHARDS+=("$name")
}

run_group "ui" \
  CaptureScreenTests OnboardingScreenTests HelpScreenTests

run_group "digitalinvoice" \
  DigitalInvoiceScreenTests DigitalInvoiceEditButtonTests

run_group "extraction" \
  ExtractionScreenTests ReviewScreenTests NoResultsTests ProductTagConfigurationTests

run_group "import" \
  ImportPdfImageTests FileImportErrorDialogTests ErrorScreenTests OpenWithTest

run_group "duedate" \
  DueDateHintBottomSheetTests SchedulePaymentBottomSheetTests

run_group "creditnote" \
  CreditNoteWarningTests \
  CreditNoteMockBackendTests

# DigitalInvoiceSkontoTests is left out on purpose — see bs_run_group_smoke.sh's header.
run_group "smokejourneys" \
  SmokeJourneyTests \
  SkontoScreenTests

echo ""
echo "=== All ${#SHARD_NAMES[@]} shards triggered. Check the BrowserStack dashboard. ==="

if [ ${#FAILED_SHARDS[@]} -gt 0 ]; then
  echo ""
  echo "=== WARNING: ${#FAILED_SHARDS[@]} shard(s) did NOT start: ${FAILED_SHARDS[*]} ==="
  echo "No BrowserStack build exists for them. Re-run each with its bs_run_group_<name>.sh."
  exit 1
fi
