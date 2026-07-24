#!/bin/bash
set -e
#
# Shard 3/4 — Extraction / review / results / product-tag tests (network extraction).
#
# Usage:
#   BS_USER="myuser" BS_KEY="mykey" ./bs_run_group_extraction.sh
#
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

export BUILD_NAME="${BUILD_NAME:-group-extraction-$(date +%Y%m%d-%H%M%S)}"

"$SCRIPT_DIR/bs_build_and_upload.sh" \
  ExtractionScreenTests \
  ReviewScreenTests \
  NoResultsTests \
  ProductTagConfigurationTests
