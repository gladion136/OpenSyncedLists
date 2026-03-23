#!/bin/bash

# Build script for desktop platforms (Linux, Windows, macOS)

set -e

echo "Building OpenSyncedLists Desktop Application..."

# Clean previous builds
cargo clean

# Build for current platform
echo "Building for current platform..."
cargo build --release

echo "Build completed! Executable location:"
if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    echo "target/release/opensyncedlists.exe"
else
    echo "target/release/opensyncedlists"
fi

# Optional: Create distribution package
if command -v tar &> /dev/null; then
    echo "Creating distribution package..."
    mkdir -p dist
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
        cp target/release/opensyncedlists.exe dist/
        tar -czf dist/opensyncedlists-desktop-windows.tar.gz -C dist opensyncedlists.exe
    else
        cp target/release/opensyncedlists dist/
        tar -czf dist/opensyncedlists-desktop-linux.tar.gz -C dist opensyncedlists
    fi
    echo "Distribution package created in dist/"
fi

echo "Desktop build complete!"
