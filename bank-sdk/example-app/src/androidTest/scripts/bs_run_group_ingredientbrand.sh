#!/bin/bash
set -e
#
# Ingredient brand shard — the Powered by Gini badge and the Gini loading indicator (PP-3478).
#
#   IngredientBrandTests            canned backend (UiTestMockBackend). The mock serves
#                                   ingredientBrandScreens and holds the analysis back a few
#                                   seconds so the Analysis screen can be checked. No network,
#                                   no server flag.
#   IngredientBrandConfigurationTests  canned backend: flag changes between SDK starts, a failing
#                                   /configurations request, and no brand on the camera,
#                                   Review, No Results and Error screens.
#   IngredientBrandLandscapeTests   canned backend, dark mode, device turned to landscape on the
#                                   camera screen: photo with and without a tip. Its QR tests
#                                   skip here like IngredientBrandQrOverlayTests.
#   IngredientBrandQrOverlayTests   needs a real QR code in front of the camera. BrowserStack
#                                   cannot inject a camera image for Espresso, so these tests
#                                   always show as SKIPPED here — that is expected, not a failure.
#                                   Run them locally (see the class KDoc).
#
# The education tests are in their own shard, bs_run_group_ingredientbrand_education.sh,
# because they need animations ON.
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_ingredientbrand.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-ingredientbrand-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  IngredientBrandTests \
  IngredientBrandConfigurationTests \
  IngredientBrandLandscapeTests \
  IngredientBrandQrOverlayTests
