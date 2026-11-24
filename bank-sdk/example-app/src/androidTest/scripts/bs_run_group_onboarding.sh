#!/bin/bash
set -e
#
# Onboarding — every test mapped to the Xray folder "/Banking Project/Onboarding Screens"
# (PP-3410…PP-3414, ticket PP-3883), as ONE BrowserStack build. The full case → test table is
# in specs/PP-3883-feature.md.
#
# Like bs_run_group_smoke.sh, this is a selection by Xray case, not a slice of the suite:
# OnboardingScreenTests also runs in the ui group. No network is needed.
#
# Every case starts from a fresh install. bs_build_and_upload.sh sends clearPackageData, so the
# onboarding shows again at the start of each test.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_onboarding.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-onboarding-$(date +%Y%m%d-%H%M%S)}"

# An array, like bs_run_group_smoke.sh, so each entry can carry the cases it covers.
ONBOARDING_TESTS=(
  # test1–test7 PP-3410 — every page in order with Next, and the Next / Skip / Get Started buttons.
  # test8a and test8 (onboarding only on the first launch) come along with the class.
  OnboardingScreenTests

  # test1 PP-3411 (swipe back and forth)       test2 PP-3412 (Skip on the first page)
  # test3 PP-3413 (Skip on the second page)    test4 PP-3414 (Skip on the third page)
  # test5 PP-3410 (Get Started on the last page opens the camera)
  OnboardingNavigationTests
)

"$SCRIPT_DIR/bs_build_and_upload.sh" "${ONBOARDING_TESTS[@]}"
