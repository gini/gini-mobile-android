#!/bin/bash
set -e
#
# Ingredient brand during the invoice education (PP-3478) — runs with animations ON.
#
#   IngredientBrandLandscapeEducationTests  the same with the device turned to landscape on the
#                                   camera screen, in dark mode.
#   IngredientBrandEducationTests   canned backend (UiTestMockBackend) with the education flag
#                                   on. Takes photos with the camera (the device rack is fine —
#                                   the mock ignores the image), because only a camera photo
#                                   triggers the invoice education.
#
# Why a separate shard: the education message is a Compose animation (1.5 s intro + 3 s
# message). With BrowserStack's disableAnimations it ends at once and the tests never see it
# (verified on a device with all animation scales at 0). So this build sends
# "disableAnimations": "false"; every other shard keeps "true".
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_ingredientbrand_education.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-ingredientbrand-education-$(date +%Y%m%d-%H%M%S)}"
export BS_DISABLE_ANIMATIONS=false

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  IngredientBrandEducationTests \
  IngredientBrandLandscapeEducationTests
