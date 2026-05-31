#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(dirname "$SCRIPT_DIR")"
PACKAGE_NAME="eu.schmidt.systems.opensyncedlists"
DEVICE_DIR="/sdcard/Android/data/${PACKAGE_NAME}/files/screenshots"
RAW_DIR="$SCRIPT_DIR/raw"
GENERATED_DIR="$SCRIPT_DIR/generated"
TEMPLATE="$SCRIPT_DIR/template/phone_screenshot.svg"
HERO_TEMPLATE="$SCRIPT_DIR/template/hero.svg"
# Pixels to crop off raw screenshots (status bar top, gesture nav bottom)
CROP_TOP=62
CROP_BOTTOM=32
FASTLANE_DIR="$REPO_ROOT/fastlane/metadata/android/en-US/images/phoneScreenshots"

# Parse arguments
CACHED=false
while [[ $# -gt 0 ]]; do
    case $1 in
        --cached)
            CACHED=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done

mkdir -p "$RAW_DIR" "$GENERATED_DIR"

require_command() {
    if ! command -v "$1" >/dev/null 2>&1; then
        echo "Missing command: $1"
        exit 1
    fi
}

device_connected() {
    adb devices | awk 'NR > 1 && $2 == "device" { found = 1 } END { exit found ? 0 : 1 }'
}

xml_escape() {
    printf '%s' "$1" \
        | sed -e 's/&/\&amp;/g' -e 's/</\&lt;/g' -e 's/>/\&gt;/g' -e 's/"/\&quot;/g'
}

render_to_png() {
    # render_to_png <svg_file> <png_file>
    inkscape "$1" \
        --export-type=png \
        --export-filename="$2" \
        --export-width=1080 \
        --export-height=2220 >/dev/null
    cp "$2" "$FASTLANE_DIR/$(basename "$2")"
}

# Build the intro / hero slide (no device screenshot, pure marketing).
render_hero() {
    local out="$1"
    local svg="$GENERATED_DIR/${out%.png}.svg"
    local png="$GENERATED_DIR/$out"
    local icon_abs="$SCRIPT_DIR/template/app_icon.png"

    sed \
        -e "s|__ICON_PATH__|${icon_abs}|g" \
        -e "s|__TAGLINE__|$(xml_escape "Your lists. Your server. Fully encrypted.")|g" \
        -e "s|__B1_TITLE__|$(xml_escape "Effortless sorting")|g" \
        -e "s|__B1_BODY__|$(xml_escape "Drag, drop or jump in seconds.")|g" \
        -e "s|__B2_TITLE__|$(xml_escape "Stay on top")|g" \
        -e "s|__B2_BODY__|$(xml_escape "Tags, progress and a compact view.")|g" \
        -e "s|__B3_TITLE__|$(xml_escape "Private sync")|g" \
        -e "s|__B3_BODY__|$(xml_escape "End-to-end encrypted on your server.")|g" \
        -e "s|__FOOTER__|$(xml_escape "Self-hosted • No tracking • Encrypted")|g" \
        "$HERO_TEMPLATE" > "$svg"

    render_to_png "$svg" "$png"
}

render_one() {
    local file="$1"
    local out="$2"
    local title="$3"
    local body="$4"
    local accent="$5"
    local raw="$RAW_DIR/$file"
    local svg="$GENERATED_DIR/${out%.png}.svg"
    local png="$GENERATED_DIR/$out"

    if [ ! -f "$raw" ]; then
        echo "Missing raw screenshot: $raw"
        exit 1
    fi

    local raw_abs
    raw_abs="$(cd "$(dirname "$raw")" && pwd)/$(basename "$raw")"

    # Crop status bar (top) and gesture nav (bottom) off the raw screenshot
    local cropped
    cropped="$(mktemp --suffix=.png)"
    trap 'rm -f "$cropped"' RETURN
    convert "$raw_abs" -gravity North -chop "0x${CROP_TOP}" \
        -gravity South -chop "0x${CROP_BOTTOM}" "$cropped"

    sed \
        -e "s|__SCREENSHOT_PATH__|${cropped}|g" \
        -e "s|__TITLE__|$(xml_escape "$title")|g" \
        -e "s|__BODY__|$(xml_escape "$body")|g" \
        -e "s|__ACCENT_COLOR__|${accent}|g" \
        "$TEMPLATE" > "$svg"

    render_to_png "$svg" "$png"
}

require_command inkscape

if [ "$CACHED" = false ]; then
    require_command adb
    if ! device_connected; then
        echo "No connected adb device found."
        exit 1
    fi

    echo "Pulling raw screenshots from device..."
    for file in \
        1_Screenshot_lists.png \
        2_Screenshot_list.png \
        3_Screenshot_list_second.png \
        4_Screenshot_lists_filter.png \
        5_Screenshot_overview.png \
        6_Screenshot_settings.png
    do
        adb pull "$DEVICE_DIR/$file" "$RAW_DIR/$file" >/dev/null
    done
else
    echo "Using cached raw screenshots..."
fi

# Remove old, differently-numbered outputs so the store gets a clean set.
rm -f "$GENERATED_DIR"/[0-9]*_Screenshot*.png "$GENERATED_DIR"/[0-9]*_Screenshot*.svg
rm -f "$FASTLANE_DIR"/[0-9]*_Screenshot*.png "$FASTLANE_DIR"/[0-9]*_*.png

echo "Rendering store screenshots..."
# Sales funnel order. Play Store sorts phone screenshots alphabetically,
# so the 01_… prefix controls the carousel order.
render_hero "01_intro.png"
render_one "1_Screenshot_lists.png"        "02_lists.png"     "Keep every list organized"     "Tags and progress at a glance." "#13c8b6"
render_one "5_Screenshot_overview.png"     "03_overview.png"  "See everything at a glance"    "Open and done items, neatly split." "#13c8b6"
render_one "2_Screenshot_list.png"         "04_actions.png"   "Reset the list in one tap"     "Uncheck or clear all done items at once." "#13c8b6"
render_one "3_Screenshot_list_second.png"  "05_sort.png"      "Sort items effortlessly"       "Drag and drop, or jump to the edge." "#111827"
render_one "4_Screenshot_lists_filter.png" "06_filter.png"    "Filter by your own tags"       "Shopping, work, projects or favorites." "#ff8a65"
render_one "6_Screenshot_settings.png"     "07_customize.png" "Make it work your way"         "Left- or right-handed, buttons your way." "#7a00ff"

echo "Generated screenshots:"
find "$GENERATED_DIR" -maxdepth 1 -name '*.png' -printf '  %f\n' | sort
echo "Fastlane screenshots replaced in $FASTLANE_DIR"
