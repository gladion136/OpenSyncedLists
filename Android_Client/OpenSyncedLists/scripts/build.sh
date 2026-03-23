#!/bin/bash
# Build script for OpenSyncedLists Android App

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

export ANDROID_HOME="$HOME/Programme/android-studio/Sdk"
export JAVA_HOME="$HOME/Programme/android-studio/jbr"
export PATH="$JAVA_HOME/bin:$ANDROID_HOME/tools:$ANDROID_HOME/platform-tools:$PATH"

cd "$PROJECT_DIR"

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
        ./gradlew test --info 2>&1 | grep -E "(> Task|PASSED|FAILED|Test |tests completed|SUCCESS|FAILURE)"

        echo ""
        echo "=========================================="
        echo "Test Report Summary"
        echo "=========================================="

        REPORT_DIR="app/build/reports/tests/testDebugUnitTest"
        if [ -d "$REPORT_DIR" ]; then
            # Parse HTML report for summary
            if [ -f "$REPORT_DIR/index.html" ]; then
                echo "Full HTML report: $PROJECT_DIR/$REPORT_DIR/index.html"
            fi

            # Show test results from XML files
            for xml in app/build/test-results/testDebugUnitTest/*.xml; do
                if [ -f "$xml" ]; then
                    CLASS=$(basename "$xml" .xml)
                    TESTS=$(grep -oP 'tests="\K[0-9]+' "$xml" 2>/dev/null || echo "0")
                    FAILURES=$(grep -oP 'failures="\K[0-9]+' "$xml" 2>/dev/null || echo "0")
                    ERRORS=$(grep -oP 'errors="\K[0-9]+' "$xml" 2>/dev/null || echo "0")
                    TIME=$(grep -oP 'time="\K[0-9.]+' "$xml" 2>/dev/null | head -1 || echo "0")

                    if [ "$FAILURES" = "0" ] && [ "$ERRORS" = "0" ]; then
                        STATUS="✓"
                    else
                        STATUS="✗"
                    fi
                    # Use LC_NUMERIC=C for consistent decimal separator
                    LC_NUMERIC=C printf "  %s %-50s %s tests (%.2fs)\n" "$STATUS" "$CLASS" "$TESTS" "$TIME"
                fi
            done

            echo ""
            # Total summary
            TOTAL_TESTS=$(find app/build/test-results -name "*.xml" -exec grep -oP 'tests="\K[0-9]+' {} \; 2>/dev/null | awk '{s+=$1} END {print s}')
            TOTAL_FAILURES=$(find app/build/test-results -name "*.xml" -exec grep -oP 'failures="\K[0-9]+' {} \; 2>/dev/null | awk '{s+=$1} END {print s}')
            echo "Total: $TOTAL_TESTS tests, $TOTAL_FAILURES failures"
        fi
        ;;
    test-verbose)
        echo "Running unit tests (verbose)..."
        ./gradlew test

        echo ""
        echo "=========================================="
        echo "Detailed Test Results"
        echo "=========================================="

        for xml in app/build/test-results/testDebugUnitTest/*.xml; do
            if [ -f "$xml" ]; then
                CLASS=$(basename "$xml" .xml | sed 's/TEST-eu.schmidt.systems.opensyncedlists.syncedlist.//')
                TESTS=$(grep -oP 'tests="\K[0-9]+' "$xml" 2>/dev/null || echo "0")
                FAILURES=$(grep -oP 'failures="\K[0-9]+' "$xml" 2>/dev/null || echo "0")

                if [ "$FAILURES" = "0" ]; then
                    echo ""
                    echo "✓ $CLASS ($TESTS tests)"
                else
                    echo ""
                    echo "✗ $CLASS ($TESTS tests, $FAILURES failed)"
                fi

                # Show individual test results
                grep -oP '<testcase name="\K[^"]+' "$xml" 2>/dev/null | while read testname; do
                    echo "    ✓ $testname"
                done
            fi
        done
        ;;
    test-class)
        if [ -z "$2" ]; then
            echo "Usage: $0 test-class <ClassName>"
            exit 1
        fi
        echo "Running tests for $2..."
        ./gradlew :app:testDebugUnitTest --tests "*$2*" --info 2>&1 | grep -E "(> Task|PASSED|FAILED|Test |tests completed|SUCCESS|FAILURE|$2)"
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
        echo "Usage: $0 {build|release|test|test-verbose|test-class <ClassName>|clean|lint}"
        echo ""
        echo "Commands:"
        echo "  build        - Build debug APK"
        echo "  release      - Build release APK"
        echo "  test         - Run unit tests with summary"
        echo "  test-verbose - Run unit tests with full output"
        echo "  test-class   - Run tests for specific class"
        echo "  clean        - Clean build artifacts"
        echo "  lint         - Run lint checks"
        exit 1
        ;;
esac
