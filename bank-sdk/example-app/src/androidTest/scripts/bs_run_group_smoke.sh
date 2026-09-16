#!/bin/bash
set -e
#
# The Xray smoke suite — the release gate, as ONE BrowserStack build.
#
# Unlike the six bs_run_group_*.sh shards, this is not a slice of the suite: it is the
# selection that covers the manual smoke test set (TC-001…TC-026, Jira PP-3415…PP-3442).
# The Xray test set is the authoritative mapping; the case each entry covers is named
# directly above it below, because only the classes written for this change carry their TC
# id in their own KDoc — the pre-existing classes predate the smoke set and name nothing.
# Adding a test here that maps to no case (or the reverse) is the one way this stops being
# usable as the gate.
#
# An entry's comment says which case it covers, not that it reproduces the manual steps
# verbatim: several cases specify a capture or a share-sheet import that no Espresso test
# can perform, and the entry then covers the case's assertions by a different import path.
#
# No camera injection: BrowserStack does not support it for Espresso (a build asking for it
# is rejected with BROWSERSTACK_INVALID_PARAMETER), so on a device the camera photographs the
# rack — and the SDK rejects those photos as not-a-document ("The document couldn't be
# accepted"). Every capture-based case therefore stays manual (TC-002, TC-023).
#
# DigitalInvoiceSkontoTests is deliberately NOT in this list. It is written and compiles,
# but no fixture yields `lineItems` and `skontoDiscounts` in one analysis result — both
# candidates were measured, each gave one of the two (see SkontoFixtures). Running it would
# add a guaranteed failure to the release gate, which is worse than an honest gap. Re-add it
# the moment a document extracts both, or if the case moves to the mock. TC-013 stays a
# manual case in Xray until then.
#
# Every test in this list runs against the real API. The mock backend (UiTestMockBackend) is
# now only used for the backend client-configuration FLAGS, by CreditNoteMockBackendTests,
# which is not in this list. The Skonto suites take real invoices instead — see SkontoFixtures
# for why two of those fixtures carry a 2028 invoice date, and for their refresh deadline.
#
# CaptureScreenTests is included even though every capture *case* is manual. It asserts only
# the camera screen itself — the flash icon and its on/off state — which needs no valid photo,
# and it passes on BrowserStack. It maps to no TC id of its own.
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

# Held in an array rather than passed as a backslash-continued list like the other
# bs_run_group_*.sh scripts: a continuation backslash has to be the last character on its
# line, so that form has nowhere to put the case id. Inside an array, each entry can carry
# the comment that says which Xray case it covers.
SMOKE_TESTS=(
  # TC-001 / PP-3415 — the onboarding pages, and that later launches skip them.
  OnboardingScreenTests

  # No TC of its own: the flash icon and its on/off state, which need no valid photo.
  CaptureScreenTests

  # TC-004 / PP-3418 (PDF), TC-003 / PP-3417 (picture), TC-016 / PP-3431 (e-invoice PDF).
  SmokeJourneyTests

  # TC-009 / PP-3423, TC-010 / PP-3424, TC-011 / PP-3425 — the standalone Skonto screen.
  SkontoScreenTests

  # TC-006 / PP-3420 — the Return Assistant item toggles and the total they change. Imports
  # the PDF from Files; the case specifies a shared picture, which Espresso cannot do.
  DigitalInvoiceScreenTests

  # TC-007 / PP-3421 — editing an item: name, quantity and unit price.
  DigitalInvoiceEditButtonTests

  # TC-020 / PP-3435 (picture) and TC-021 / PP-3436 (PDF) — the no-results screen.
  NoResultsTests

  # TC-022 / PP-3437 — the no-internet error screen. Assumes itself out on BrowserStack,
  # where disabling wifi leaves the device online, so the case stays manual there.
  ErrorScreenTests#test2_verifyNetworkErrorScreen

  # TC-024 / PP-3439 — a password-protected PDF is refused with the error dialog.
  FileImportErrorDialogTests#test1_importPasswordProtectedFileAndVerifyErrorDialogIsDisplayed

  # TC-025 / PP-3440 — choosing more than ten pictures is refused with the limit message.
  FileImportErrorDialogTests#test3_selectingMoreThanTenPicturesIsRefusedWithTheLimitMessage
)

"$SCRIPT_DIR/bs_build_and_upload.sh" "${SMOKE_TESTS[@]}"
