#!/bin/bash
set -e
#
# Error and No Results screens — every test mapped to the Xray folder
# "/Banking Project/QAlity imported test cases/Error & No result screens" (PP-2895…PP-2917,
# ticket PP-3918), as ONE BrowserStack build. The full case → test table is in
# specs/PP-3918-feature.md.
#
# Like bs_run_group_smoke.sh, this is a selection by Xray case, not a slice of the suite: the
# classes below also run in other groups (import, extraction). Espresso `class` filters match
# EXACTLY, so a Class#method entry runs that method alone.
#
# Three tests skip themselves on BrowserStack, on purpose: ErrorScreenTests#test2, #test3 and
# #test4 switch the device network off, which BrowserStack blocks. Their cases (PP-2895, PP-2902,
# PP-2903) are also covered here by the mock-backend tests, which need no network switch.
#
# The "Login Error Screens" cases (PP-2911 – PP-2917) share the mock-backend tests of the
# matching error type; the login request itself stays a manual Charles check.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_errorscreens.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-errorscreens-$(date +%Y%m%d-%H%M%S)}"

# An array, like bs_run_group_smoke.sh, so each entry can carry the cases it covers.
ERROR_SCREEN_TESTS=(
  # Canned backend (UiTestMockBackend): each test forces one error screen, or No Results.
  #   test1 PP-2895, PP-2903 (no internet)      test2 PP-2896, PP-2901, PP-2911, PP-2914 (400)
  #   test3 PP-2897, PP-2912 (401)               test4 PP-2898, PP-2917 (generic)
  #   test5 PP-2899, PP-2916 (502)               test6 PP-2900, PP-2915 (503)
  #   test7 PP-2913 (500)                        test8 PP-2902 (Back To Camera)
  #   test9 PP-2904 (No Results after a camera photo)
  ErrorAndNoResultScreenMockBackendTests

  # Whole classes, not single methods: BrowserStack rejects a `class` filter of 1000 characters
  # or more ([BROWSERSTACK_INVALID_SYNTAX]), and every entry carries the full package name. The
  # method list came to 1260. Two extra tests come along, outside this test set:
  # ErrorScreenTests#test3 (skips itself on BrowserStack) and FileImportErrorDialogTests#test3
  # (the 10-picture limit, smoke PP-3440).

  # test1 PP-2896 (upload error, real backend). test2 PP-2895, PP-2903 and test4 PP-2902 switch
  # the network off, so they skip on BrowserStack.
  ErrorScreenTests

  # test1 PP-2904 (picture), test2 PP-2907 (PDF) — No Results for a document that is not an invoice.
  NoResultsTests

  # test1 PP-2906 (password-protected), test2 PP-2909 (too many pages), test4 PP-2908 (over 10 MB).
  FileImportErrorDialogTests

  # PP-2910 — unsupported file type never starts the SDK flow.
  "OpenWithTest#opening_unsupported_file_with_MainActivity_does_not_start_Bank_SDK"
)

"$SCRIPT_DIR/bs_build_and_upload.sh" "${ERROR_SCREEN_TESTS[@]}"
