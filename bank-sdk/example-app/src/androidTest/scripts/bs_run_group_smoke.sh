#!/bin/bash
set -e
#
# The Xray smoke suite — the release gate, as ONE BrowserStack build.
#
# Unlike the six bs_run_group_*.sh shards, this is not a slice of the suite: it is the
# selection that covers the manual smoke test set (TC-001…TC-026, Jira PP-3415…PP-3442).
# The authoritative mapping lives in ../COVERAGE.md — every entry below is a row marked
# `automated` there, and the two must be changed together. Adding a test here without a
# COVERAGE.md row (or the reverse) is the one way this stops being usable as the gate.
#
# No camera injection: BrowserStack does not support it for Espresso (a build asking for it
# is rejected with BROWSERSTACK_INVALID_PARAMETER), so on a device the camera photographs the
# rack — and the SDK rejects those photos as not-a-document ("The document couldn't be
# accepted"). Every capture-based case therefore stays manual (TC-002, TC-023) — see
# ../COVERAGE.md.
#
# DigitalInvoiceSkontoTests is deliberately NOT in this list. It is written and compiles,
# but no fixture yields `lineItems` and `skontoDiscounts` in one analysis result — three
# candidates were measured, each gave one of the two (see SkontoFixtures). Running it would
# add a guaranteed failure to the release gate, which is worse than an honest gap. Re-add it
# the moment a document extracts both, or if the case moves to the mock. TC-013 is `manual`
# in ../COVERAGE.md until then.
#
# Every test in this list runs against the real API. The mock backend (UiTestMockBackend) is
# now only used for the backend client-configuration FLAGS, by CreditNoteMockBackendTests,
# which is not in this list. The Skonto suites take real invoices instead — see SkontoFixtures
# for why two of those fixtures carry a 2028 invoice date, and for their refresh deadline.
#
# CaptureScreenTests is included even though every capture *case* is manual. It asserts only
# the camera screen itself — the flash icon and its on/off state — which needs no valid photo,
# and it passes on BrowserStack. It maps to no TC id; see "Camera screen" in ../COVERAGE.md.
#
# Espresso `class` filters match EXACTLY, so a Class#method entry runs that method alone
# and needs no test renaming. (The iOS suite has the opposite problem — XCUITest matches
# only-testing entries by name prefix — so don't carry that warning over from the iOS
# scripts.) The local-run gotcha is different again and applies to ./gradlew, not here:
# two comma-separated class names in -Pandroid.testInstrumentationRunnerArguments.class
# start 0 tests under the Test Orchestrator.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_smoke.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-smoke-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  OnboardingScreenTests \
  CaptureScreenTests \
  SmokeJourneyTests \
  SkontoScreenTests \
  DigitalInvoiceScreenTests \
  DigitalInvoiceEditButtonTests \
  NoResultsTests \
  ErrorScreenTests#test2_verifyNetworkErrorScreen \
  FileImportErrorDialogTests#test1_importPasswordProtectedFileAndVerifyErrorDialogIsDisplayed \
  FileImportErrorDialogTests#test3_selectingMoreThanTenPicturesIsRefusedWithTheLimitMessage
