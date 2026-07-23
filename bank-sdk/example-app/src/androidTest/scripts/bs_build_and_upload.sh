#!/bin/bash
set -e

# ── Usage ────────────────────────────────────────────────────────────────────────
# ./bs_build_and_upload.sh [TARGET ...]
#
# TARGET  Zero or more test classes or Class#method entries to run.
#         Short names are auto-prefixed with the test package; fully-qualified names
#         are used as-is. With no target, the full test package runs.
#         Examples: CaptureScreenTests
#                   HelpScreenTests#test7_clickHelpButtonAndVerifyContentOnHelpScreen
#
# For sharded runs, prefer the preset wrappers in this directory:
#   bs_run_group_ui.sh            – Capture / Onboarding / MainScreen / Help
#   bs_run_group_digitalinvoice.sh – DigitalInvoice screens (isolated)
#   bs_run_group_extraction.sh    – Extraction / Review / NoResults / ProductTag
#   bs_run_group_import.sh        – Import / FileImportError / ErrorScreen / OpenWith
#   bs_run_all_groups.sh          – builds+uploads ONCE, then triggers all four shards
#
# BrowserStack credentials must be set via environment variables:
#   export BS_USER="your_username"
#   export BS_KEY="your_access_key"
#
# Advanced env vars (used by bs_run_all_groups.sh to avoid rebuilding/re-uploading):
#   APP_URL, TEST_URL, IMAGE_URL, PDF_URL, SAMPLE_PDF_URL
#       If all five are set, the Gradle build and BrowserStack uploads are SKIPPED and
#       these pre-uploaded artifacts are reused.
#   ARTIFACT_URLS_FILE  If set, the five artifact URLs are written to this file (as
#       `NAME=value` lines) after upload, so a caller can source and reuse them.
#   SKIP_TRIGGER=true   Do the build + upload but do NOT trigger a test run.
#
# Examples:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_build_and_upload.sh
#   BS_USER="myuser" BS_KEY="mykey" ./bs_build_and_upload.sh CaptureScreenTests NoResultsTests

# ── Configuration ────────────────────────────────────────────────────────────────
BS_USER="${BS_USER:-<your_browserstack_user_name>}"
BS_KEY="${BS_KEY:-<your_browserstack_access_key>}"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../../../../.." && pwd)"

FLAVOR="devExampleApp"
BUILD_TYPE="debug"
APK_DIR="$REPO_ROOT/bank-sdk/example-app/build/outputs/apk"

APP_APK="$APK_DIR/$FLAVOR/$BUILD_TYPE/example-app-dev-exampleApp-debug.apk"
TEST_APK="$APK_DIR/androidTest/$FLAVOR/$BUILD_TYPE/example-app-dev-exampleApp-debug-androidTest.apk"

TEST_DOCUMENTS="$SCRIPT_DIR/../testDocuments"
TEST_IMAGE="$TEST_DOCUMENTS/test_image.jpeg"
TEST_PDF="$TEST_DOCUMENTS/Testrechnung-RA-1.pdf"
SAMPLE_PDF="$TEST_DOCUMENTS/sample.pdf"

DEVICE_1="Google Pixel 9-16.0"
DEVICE_2="Google Pixel 10 Pro-16.0"

# Pass one or more test targets as arguments (classes or Class#method), or leave empty
# to run the whole package. Short names are auto-prefixed with the test package, e.g.:
#   NoResultsTests
#   HelpScreenTests#test7_clickHelpButtonAndVerifyContentOnHelpScreen
TEST_PACKAGE="net.gini.android.bank.sdk.exampleapp.ui.testcases"
TEST_TARGETS=("$@")

# ── Validate credentials ─────────────────────────────────────────────────────────
if [[ "$BS_USER" == "<your_browserstack_user_name>" ]] || [[ "$BS_KEY" == "<your_browserstack_access_key>" ]]; then
  echo "Error: BrowserStack credentials not set."
  echo "  export BS_USER=\"your_username\""
  echo "  export BS_KEY=\"your_access_key\""
  exit 1
fi

# ── Reuse pre-uploaded artifacts if all five URLs are provided ────────────────────
if [ -n "$APP_URL" ] && [ -n "$TEST_URL" ] && [ -n "$IMAGE_URL" ] && [ -n "$PDF_URL" ] && [ -n "$SAMPLE_PDF_URL" ]; then
  echo "Reusing pre-uploaded artifacts (skipping Gradle build + upload)."
  echo "  app_url:          $APP_URL"
  echo "  test_suite_url:   $TEST_URL"
else
  # ── Validate media files ───────────────────────────────────────────────────────
  if [ ! -f "$TEST_IMAGE" ]; then
    echo "Error: Test image not found: $TEST_IMAGE"
    exit 1
  fi
  echo "Using test image: $TEST_IMAGE"

  if [ ! -f "$TEST_PDF" ]; then
    echo "Error: Test PDF not found: $TEST_PDF"
    exit 1
  fi
  echo "Using test PDF:   $TEST_PDF"

  if [ ! -f "$SAMPLE_PDF" ]; then
    echo "Error: Sample PDF not found: $SAMPLE_PDF"
    exit 1
  fi
  echo "Using sample PDF: $SAMPLE_PDF"

  # ── Step 1: Build APKs ─────────────────────────────────────────────────────────
  echo "[1/4] Building APKs..."
  cd "$REPO_ROOT"
  ./gradlew \
    :bank-sdk:example-app:assembleDevExampleAppDebug \
    :bank-sdk:example-app:assembleDevExampleAppDebugAndroidTest
  echo "Build complete"

  # ── Step 2: Validate APK outputs ─────────────────────────────────────────────────
  if [ ! -f "$APP_APK" ]; then
    echo "Error: App APK not found: $APP_APK"
    exit 1
  fi
  if [ ! -f "$TEST_APK" ]; then
    echo "Error: Test APK not found: $TEST_APK"
    exit 1
  fi
  echo "App APK:  $APP_APK"
  echo "Test APK: $TEST_APK"

  # ── Step 3: Upload to BrowserStack ──────────────────────────────────────────────
  echo "[3/4] Uploading to BrowserStack..."

  echo "  Uploading test image..."
  IMAGE_RESPONSE=$(curl -s -u "$BS_USER:$BS_KEY" \
    -X POST "https://api-cloud.browserstack.com/app-automate/upload-media" \
    -F "file=@$TEST_IMAGE" \
    -F "custom_id=CaptureInjectionImage")
  echo "  Image response: $IMAGE_RESPONSE"
  IMAGE_URL=$(echo "$IMAGE_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['media_url'])" 2>/dev/null || true)
  if [ -z "$IMAGE_URL" ]; then echo "Error: Failed to get media_url — check response above"; exit 1; fi

  echo "  Uploading test PDF..."
  PDF_RESPONSE=$(curl -s -u "$BS_USER:$BS_KEY" \
    -X POST "https://api-cloud.browserstack.com/app-automate/upload-media" \
    -F "file=@$TEST_PDF" \
    -F "custom_id=UploadPDF")
  echo "  PDF response: $PDF_RESPONSE"
  PDF_URL=$(echo "$PDF_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['media_url'])" 2>/dev/null || true)
  if [ -z "$PDF_URL" ]; then echo "Error: Failed to get PDF media_url — check response above"; exit 1; fi

  echo "  Uploading sample PDF..."
  SAMPLE_PDF_RESPONSE=$(curl -s -u "$BS_USER:$BS_KEY" \
    -X POST "https://api-cloud.browserstack.com/app-automate/upload-media" \
    -F "file=@$SAMPLE_PDF" \
    -F "custom_id=SamplePDF")
  echo "  Sample PDF response: $SAMPLE_PDF_RESPONSE"
  SAMPLE_PDF_URL=$(echo "$SAMPLE_PDF_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['media_url'])" 2>/dev/null || true)
  if [ -z "$SAMPLE_PDF_URL" ]; then echo "Error: Failed to get sample PDF media_url — check response above"; exit 1; fi

  echo "  Uploading app APK..."
  APP_RESPONSE=$(curl -s -u "$BS_USER:$BS_KEY" \
    -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/app" \
    -F "file=@$APP_APK")
  echo "  App response: $APP_RESPONSE"
  APP_URL=$(echo "$APP_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['app_url'])" 2>/dev/null || true)
  if [ -z "$APP_URL" ]; then echo "Error: Failed to get app_url — check response above"; exit 1; fi

  echo "  Uploading test suite APK..."
  TEST_RESPONSE=$(curl -s -u "$BS_USER:$BS_KEY" \
    -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/test-suite" \
    -F "file=@$TEST_APK")
  echo "  Test suite response: $TEST_RESPONSE"
  TEST_URL=$(echo "$TEST_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin)['test_suite_url'])" 2>/dev/null || true)
  if [ -z "$TEST_URL" ]; then echo "Error: Failed to get test_suite_url — check response above"; exit 1; fi

  echo "All files uploaded"
  echo "  app_url:          $APP_URL"
  echo "  test_suite_url:   $TEST_URL"
  echo "  image media_url:  $IMAGE_URL"
  echo "  pdf media_url:    $PDF_URL"
  echo "  sample pdf url:   $SAMPLE_PDF_URL"
fi

# ── Optionally export the artifact URLs for reuse by a caller ─────────────────────
if [ -n "$ARTIFACT_URLS_FILE" ]; then
  {
    echo "APP_URL=$APP_URL"
    echo "TEST_URL=$TEST_URL"
    echo "IMAGE_URL=$IMAGE_URL"
    echo "PDF_URL=$PDF_URL"
    echo "SAMPLE_PDF_URL=$SAMPLE_PDF_URL"
  } > "$ARTIFACT_URLS_FILE"
  echo "Wrote artifact URLs to $ARTIFACT_URLS_FILE"
fi

# ── Optionally stop here (build + upload only, no test run) ───────────────────────
if [ "$SKIP_TRIGGER" = "true" ]; then
  echo "SKIP_TRIGGER=true — build + upload done, not triggering a test run."
  exit 0
fi

# ── Step 4: Trigger test run ─────────────────────────────────────────────────────
echo "[4/4] Triggering test build on BrowserStack..."

# Build the test filter JSON fragment.
# With targets: run only those classes/methods. Without: run the whole package.
if [ ${#TEST_TARGETS[@]} -gt 0 ]; then
  CLASS_JSON=""
  for target in "${TEST_TARGETS[@]}"; do
    case "$target" in
      *.*) fq="$target" ;;               # already fully-qualified
      *)   fq="$TEST_PACKAGE.$target" ;;  # short name -> prepend package
    esac
    if [ -n "$CLASS_JSON" ]; then CLASS_JSON="$CLASS_JSON, "; fi
    CLASS_JSON="$CLASS_JSON\"$fq\""
  done
  FILTER_JSON="\"class\": [$CLASS_JSON],"
  echo "Running targets: $CLASS_JSON"
else
  FILTER_JSON="\"package\": [\"$TEST_PACKAGE\"],"
fi

BUILD_NAME="${BUILD_NAME:-local-$(date +%Y%m%d-%H%M%S)}"

BUILD_RESPONSE=$(curl -s -u "$BS_USER:$BS_KEY" \
  -X POST "https://api-cloud.browserstack.com/app-automate/espresso/v2/build" \
  -H "Content-Type: application/json" \
  -d "{
    \"project\": \"gini-mobile-android\",
    \"name\": \"$BUILD_NAME\",
    \"devices\": [\"$DEVICE_1\", \"$DEVICE_2\"],
    \"app\": \"$APP_URL\",
    \"testSuite\": \"$TEST_URL\",
    \"singleRunnerInvocation\": \"true\",
    \"useOrchestrator\": \"true\",
    \"clearPackageData\": \"true\",
    \"disableAnimations\": \"true\",
    $FILTER_JSON
    \"uploadMedia\": [\"$IMAGE_URL\", \"$PDF_URL\", \"$SAMPLE_PDF_URL\"]
  }")
echo "Build response: $BUILD_RESPONSE"
echo ""
echo "Done! Check BrowserStack App Automate dashboard for results."
