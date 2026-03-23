#!/bin/bash

# Build script for web platform using dx (Dioxus CLI)

set -e

echo "Building OpenSyncedLists Web Application..."

# Check if dx is installed
if ! command -v dx &> /dev/null; then
    echo "dx not found. Installing..."
    cargo install dioxus-cli
fi

# Add wasm32 target if not present
rustup target add wasm32-unknown-unknown

# Build for web
echo "Building with dx..."
dx build --platform web --release

echo "Web build completed!"
echo "To serve the web application:"
echo "  dx serve --platform web"
echo "  Then open http://localhost:8080"
