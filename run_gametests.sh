#!/usr/bin/env bash
# Run Forge game test server and tee output to a log file.
# Usage: ./run_gametests.sh [log_file]
LOG="${1:-gametest.log}"
./gradlew runGameTestServer 2>&1 | tee "$LOG"
