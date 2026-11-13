#!/bin/bash
set -e
#
# Fraud prevention warning tests (PP-3970): the IBAN mismatch warning and the IBAN selection
# sheet of the capture SDK.
#
# Every class runs against the canned backend (UiTestMockBackend), so no network, no server
# flag and no special document is needed. The mock serves the `fraudSignalsEnabled` client flag
# and the `fraudSignals` compound extraction; the uploaded image (test_image.jpeg) only gives the
# photo picker something to select, and the landscape class imports sample.pdf instead.
#
#   FraudSignalsFlagTests        5 tests: when the warning must NOT appear (flag off, unknown
#                                code, no fraudSignals, ordinary invoice, analysis error).
#   FraudWarningContentTests     8 tests: texts of the three warning types, several printed
#                                IBANs, the protection drop-down on every type.
#   FraudWarningActionsTests     9 tests: back press on every type, "Cancel transfer",
#                                "Proceed anyway".
#   FraudFeedbackTests           4 tests: the feedback after the warning — chosen IBAN, BIC and
#                                no fraudSignals — for every type.
#   IbanSelectionSheetTests      6 tests: the selection sheet: content, back, error, chosen
#                                IBAN + BIC.
#   FraudWarningLandscapeTests   9 tests: dark mode, device turned to landscape on the camera
#                                screen: warning content, drop-down, both buttons, selection
#                                content and "Continue". Skipped where dark mode cannot be set.
#
# 41 tests in total. Each class is a separate argument; bs_build_and_upload.sh passes them to
# BrowserStack as its `class` list. (For a local adb run under the Test Orchestrator, filter ONE
# class at a time — two comma-separated classes start 0 tests.)
#
# The outside tap of TC-017 stays manual: the full-height warning leaves no scrim to tap on a
# phone, only the system status bar.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_fraud.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-fraud-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  FraudSignalsFlagTests \
  FraudWarningContentTests \
  FraudWarningActionsTests \
  FraudFeedbackTests \
  IbanSelectionSheetTests \
  FraudWarningLandscapeTests
