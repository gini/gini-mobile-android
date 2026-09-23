#!/bin/bash
set -e
#
# Release sign-off run. Triggers the shards in the CURRENT RELEASE's test scope as
# separate BrowserStack builds, building the APKs and uploading them (and the media)
# only ONCE — the same trick bs_run_all_groups.sh uses.
#
# This is the ONLY script that leaves the everyday "gini-mobile-android" project.
# It puts its builds in a project of their own, named after the release being prepared
# (GiniBankSDK-Android-<next version>), so that release's sign-off sits together in the
# App Automate dashboard and is still there months after it ships. Day-to-day runs —
# bs_build_and_upload.sh and every bs_run_group_*.sh — stay in gini-mobile-android.
#
# ── WHERE THE VERSION COMES FROM ────────────────────────────────────────────────
# The RELEASE_VERSION constant below: the version the NEXT release is planned as, set
# by hand from the release plan. It ships as the placeholder "4.x.x" and the script
# REFUSES TO RUN until you replace it — see the block around it for how.
#
# Nothing is derived from the branch or from gradle.properties. Both describe the
# release just gone, not the one being prepared: on the release branch gradle.properties
# still holds the previous version when the suite runs, because the bump has not landed.
#
# For a one-off run under a different name, pass the project instead of editing:
#   BS_PROJECT="GiniBankSDK-Android-4.5.0-RC1" ./bs_run_release.sh
#
# Scope — the features the release being signed off ships or changes.
# Keep this list in step with the RELEASE SCOPE block further down:
#   duedate     – Due Date Hint / Schedule Payment bottom sheets
#   creditnote  – Credit Note warning bottom sheet
#   import      – file upload: pdf/image import, file-import errors, open-with
#
# ── REUSING THIS SCRIPT FOR THE NEXT RELEASE ────────────────────────────────────
# Two edits, both by hand — neither the version nor the scope is something a script can
# work out for itself:
#
#   1. RELEASE_VERSION below — set it to the version the next release is planned as
#      (it ships as the placeholder "4.x.x"; the script stops with instructions if
#      you forget).
#
#   2. The scope. Edit the run_group calls in the RELEASE SCOPE block below — add,
#      remove or change shards to match what the release actually touches, and update
#      the "Scope" list in the header above so it stays honest.
#
# The test class names to use are the same ones bs_run_all_groups.sh lists; a shard
# here can hold any subset of them. To run the ENTIRE suite instead of a scope, use
# bs_run_all_groups.sh — leave this script for release sign-off.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_release.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
STAMP="$(date +%Y%m%d-%H%M%S)"

# ══ RELEASE VERSION ═════════════════════════════════════════════════════════════
#
# The version of the NEXT release — the one being prepared, the one this sign-off is
# clearing for shipping. NOT the version that is already out, and NOT the version in
# bank-sdk/sdk/gradle.properties: that still holds the PREVIOUS version while the suite
# runs, because the version bump has not landed on the release branch yet. The branch
# name does not carry it either — release/bank-sdk-4.5 is only the minor line.
#
# So it is a decision, taken from the release plan, and set here by hand. It ships as a
# PLACEHOLDER and the script refuses to run until you replace it, so a sign-off can
# never land in a project called "4.x.x".
#
# HOW TO USE IT
#
#   1. Look up the version the next release is planned as — the release ticket / RC
#      ticket says it. Say that is 4.6.0. Change the line below to:
#
#        RELEASE_VERSION="4.6.0"
#
#      Use the full version, including the patch: 4.5.0, 4.5.1, 4.6.0 — not 4.6.
#      Your builds then appear in the App Automate project "GiniBankSDK-Android-4.6.0",
#      ready and waiting before 4.6.0 actually ships.
#
#   2. Commit that change with the scope edit below, so the next person on the release
#      branch runs the right thing without asking anyone.
#
#   3. For a one-off run under a different name — an RC, or a re-run you want kept
#      apart — do NOT edit the file. Pass the project instead; it wins over this line:
#
#        BS_USER="u" BS_KEY="k" BS_PROJECT="GiniBankSDK-Android-4.6.0-RC1" ./bs_run_release.sh
#
RELEASE_VERSION="4.x.x"
#
# ════════════════════════════════════════════════════════════════════════════════

# Project name in the App Automate dashboard: BS_PROJECT from the environment if the
# caller set one, otherwise built from RELEASE_VERSION above.
if [ -z "$BS_PROJECT" ]; then
  case "$RELEASE_VERSION" in
    *[xX]*|"")
      echo "Error: RELEASE_VERSION is still the placeholder \"$RELEASE_VERSION\"."
      echo "       This script names its BrowserStack project after the release, so an"
      echo "       unset version would file the sign-off under a project nobody looks in."
      echo ""
      echo "       Either set it in $0 — e.g. RELEASE_VERSION=\"4.6.0\" — and commit that,"
      echo "       or name the project for this run only:"
      echo "         BS_PROJECT=\"GiniBankSDK-Android-4.6.0\" $0"
      exit 1
      ;;
  esac
  BS_PROJECT="GiniBankSDK-Android-$RELEASE_VERSION"
fi
export BS_PROJECT

echo "Release project: $BS_PROJECT"

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
#   1. Update the "Scope" list in the header above.
#   2. Check RELEASE_VERSION above is the version you are signing off FOR — the next
#      release. Not "4.x.x", and not the version that already shipped.
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
echo "=== Release scope triggered. Check the \"$BS_PROJECT\" project in the BrowserStack dashboard. ==="
