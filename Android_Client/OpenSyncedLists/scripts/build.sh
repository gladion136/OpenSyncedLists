#!/bin/bash
# Build script for OpenSyncedLists Android App

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

ANDROID_HOME_CANDIDATES=(
    "$HOME/Programme/android-studio/Sdk"
    "$HOME/Android/Sdk"
    "$HOME/Programme"
)
export ANDROID_HOME="${ANDROID_HOME_CANDIDATES[0]}"
for sdk_dir in "${ANDROID_HOME_CANDIDATES[@]}"; do
    if [ -x "$sdk_dir/platform-tools/adb" ]; then
        export ANDROID_HOME="$sdk_dir"
        break
    fi
done
export JAVA_HOME="$HOME/Programme/android-studio/jbr"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH"

UNIT_RESULT_DIR="app/build/test-results/testDebugUnitTest"
UI_RESULT_DIR="app/build/outputs/androidTest-results/connected"
REPO_ROOT="$(cd "$PROJECT_DIR/../.." && pwd)"

cd "$PROJECT_DIR"

# ---- Helpers ----------------------------------------------------------------

# Run gradlew and filter output to key lines only.
run_gradle() {
    ./gradlew "$@" --info 2>&1 | grep -E "(> Task|PASSED|FAILED|Test |tests completed|SUCCESS|FAILURE)"
}

# Print a table row for each XML file matching the given glob.
# Usage: print_xml_rows <glob> <class_name_sed> <col_width>
print_xml_rows() {
    local glob="$1" name_sed="$2" col_width="$3"
    for xml in $glob; do
        [ -f "$xml" ] || continue
        local CLASS TESTS FAILURES ERRORS TIME STATUS
        CLASS=$(basename "$xml" .xml)
        [ -n "$name_sed" ] && CLASS=$(echo "$CLASS" | sed "$name_sed")
        TESTS=$(grep -oP 'tests="\K[0-9]+'    "$xml" 2>/dev/null || echo "0")
        FAILURES=$(grep -oP 'failures="\K[0-9]+' "$xml" 2>/dev/null || echo "0")
        ERRORS=$(grep -oP 'errors="\K[0-9]+'   "$xml" 2>/dev/null || echo "0")
        TIME=$(grep -oP 'time="\K[0-9.]+'      "$xml" 2>/dev/null | head -1 || echo "0")
        if [ "$FAILURES" = "0" ] && [ "$ERRORS" = "0" ]; then STATUS="✓"; else STATUS="✗"; fi
        LC_NUMERIC=C printf "  %s %-${col_width}s %s tests (%.2fs)\n" \
            "$STATUS" "$CLASS" "$TESTS" "$TIME"
    done
}

# Print "Label: N tests, M failures" by summing all XMLs under <dir>.
print_totals() {
    local dir="$1" label="$2"
    local TOTAL FAILURES
    TOTAL=$(find "$dir" -name "*.xml" -exec grep -oP 'tests="\K[0-9]+'    {} \; 2>/dev/null \
        | awk '{s+=$1} END {print s+0}')
    FAILURES=$(find "$dir" -name "*.xml" -exec grep -oP 'failures="\K[0-9]+' {} \; 2>/dev/null \
        | awk '{s+=$1} END {print s+0}')
    echo "${label}: ${TOTAL} tests, ${FAILURES} failures"
}

# Print the unit-test result table.
show_unit_results() {
    local col_width="${1:-50}"
    print_xml_rows "$UNIT_RESULT_DIR/*.xml" "" "$col_width"
}

# Print the UI-test result table.
show_ui_results() {
    local col_width="${1:-60}"
    print_xml_rows "$UI_RESULT_DIR/**/*.xml $UI_RESULT_DIR/*.xml" "s/TEST-//" "$col_width"
}

# ---- Commands ---------------------------------------------------------------

case "${1:-build}" in
    build)
        echo "Building debug APK..."
        ./gradlew assembleDebug
        ;;
    release)
        echo "Building release APK..."
        ./gradlew assembleRelease
        ;;
    test)
        echo "Running unit tests..."
        run_gradle test

        echo ""
        echo "=========================================="
        echo "Test Report Summary"
        echo "=========================================="

        if [ -d "$UNIT_RESULT_DIR" ]; then
            [ -f "app/build/reports/tests/testDebugUnitTest/index.html" ] && \
                echo "Full HTML report: $PROJECT_DIR/app/build/reports/tests/testDebugUnitTest/index.html"
            show_unit_results 50
            echo ""
            print_totals "$UNIT_RESULT_DIR" "Total"
        fi
        ;;
    test-verbose)
        echo "Running unit tests (verbose)..."
        ./gradlew test

        echo ""
        echo "=========================================="
        echo "Detailed Test Results"
        echo "=========================================="

        for xml in "$UNIT_RESULT_DIR"/*.xml; do
            [ -f "$xml" ] || continue
            CLASS=$(basename "$xml" .xml | sed 's/TEST-eu.schmidt.systems.opensyncedlists.syncedlist.//')
            TESTS=$(grep -oP 'tests="\K[0-9]+'    "$xml" 2>/dev/null || echo "0")
            FAILURES=$(grep -oP 'failures="\K[0-9]+' "$xml" 2>/dev/null || echo "0")
            echo ""
            if [ "$FAILURES" = "0" ]; then
                echo "✓ $CLASS ($TESTS tests)"
            else
                echo "✗ $CLASS ($TESTS tests, $FAILURES failed)"
            fi
            grep -oP '<testcase name="\K[^"]+' "$xml" 2>/dev/null | while read -r testname; do
                echo "    ✓ $testname"
            done
        done
        ;;
    test-class)
        if [ -z "$2" ]; then
            echo "Usage: $0 test-class <ClassName>"
            exit 1
        fi
        echo "Running tests for $2..."
        ./gradlew :app:testDebugUnitTest --tests "*$2*" --info 2>&1 \
            | grep -E "(> Task|PASSED|FAILED|Test |tests completed|SUCCESS|FAILURE|$2)"
        ;;
    test-ui)
        echo "Running UI / instrumented tests (requires connected device or emulator)..."
        run_gradle connectedAndroidTest

        echo ""
        echo "=========================================="
        echo "UI Test Report Summary"
        echo "=========================================="

        if [ -d "$UI_RESULT_DIR" ]; then
            [ -f "app/build/reports/androidTests/connected/index.html" ] && \
                echo "Full HTML report: $PROJECT_DIR/app/build/reports/androidTests/connected/index.html"
            show_ui_results 60
            echo ""
            print_totals "$UI_RESULT_DIR" "Total"
        else
            echo "No UI test results found. Make sure a device/emulator is connected."
        fi
        ;;
    gen-screenshots)
        echo "Generating Play Store screenshots (requires connected device or emulator)..."
        command -v adb >/dev/null 2>&1 || {
            echo "adb not found. Check ANDROID_HOME in this script."
            exit 1
        }
        command -v inkscape >/dev/null 2>&1 || {
            echo "inkscape not found. Install Inkscape and ensure it is on PATH."
            exit 1
        }
        if ! adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit found ? 0 : 1 }'; then
            echo "No connected adb device found."
            exit 1
        fi

        ./gradlew :app:assembleDebug :app:assembleDebugAndroidTest

        adb install -r -t app/build/outputs/apk/debug/app-debug.apk >/dev/null
        adb install -r -t app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk >/dev/null
        adb shell cmd locale set-app-localeconfig eu.schmidt.systems.opensyncedlists --locales en-US >/dev/null 2>&1 || true
        adb shell cmd locale set-app-locales eu.schmidt.systems.opensyncedlists --locales en-US >/dev/null 2>&1 || true
        adb shell am instrument -w \
            -e class eu.schmidt.systems.opensyncedlists.screenshots.ScreenshotGenerationTest \
            eu.schmidt.systems.opensyncedlists.test/androidx.test.runner.AndroidJUnitRunner

        "$REPO_ROOT/screenshots/generate.sh"
        ;;
    test-all)
        echo "Running all tests: unit tests + UI / instrumented tests..."
        echo ""

        echo "--- Unit Tests ---"
        run_gradle test

        echo ""
        echo "--- UI Tests (requires connected device or emulator) ---"
        run_gradle connectedAndroidTest

        echo ""
        echo "=========================================="
        echo "Combined Test Report Summary"
        echo "=========================================="

        echo ""
        echo "Unit Tests:"
        show_unit_results 60
        print_totals "$UNIT_RESULT_DIR" "Unit"

        echo ""
        echo "UI Tests:"
        if [ -d "$UI_RESULT_DIR" ]; then
            show_ui_results 60
            print_totals "$UI_RESULT_DIR" "UI"
        else
            echo "  (no UI test results – device/emulator not connected)"
        fi
        ;;
    clean)
        echo "Cleaning build..."
        ./gradlew clean
        ;;
    lint)
        echo "Running lint..."
        ./gradlew lint
        ;;
    *)
        echo "Usage: $0 {build|release|test|test-verbose|test-class <ClassName>|test-ui|gen-screenshots|test-all|clean|lint}"
        echo ""
        echo "Commands:"
        echo "  build        - Build debug APK"
        echo "  release      - Build release APK"
        echo "  test         - Run unit tests with summary"
        echo "  test-verbose - Run unit tests with full output"
        echo "  test-class   - Run tests for specific class"
        echo "  test-ui      - Run UI / instrumented tests (needs device or emulator)"
        echo "  gen-screenshots - Generate screenshots and replace fastlane images"
        echo "  test-all     - Run unit tests + UI tests"
        echo "  clean        - Clean build artifacts"
        echo "  lint         - Run lint checks"
        exit 1
        ;;
esac
