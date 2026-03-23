.PHONY: all build build-desktop build-web test clean install run dev help

# Default target
all: build

# Build for all platforms
build: build-desktop build-web

# Build desktop application
build-desktop:
	@echo "Building desktop application..."
	@chmod +x scripts/build_desktop.sh
	@./scripts/build_desktop.sh

# Build web application
build-web:
	@echo "Building web application..."
	@chmod +x scripts/build_web.sh
	@./scripts/build_web.sh

# Run tests
test:
	@echo "Running tests..."
	cargo test

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."
	cargo clean
	rm -rf pkg/
	rm -rf web/
	rm -rf dist/

# Install dependencies
install:
	@echo "Installing dependencies..."
	rustup target add wasm32-unknown-unknown
	cargo install wasm-pack

# Run desktop application in development mode
run:
	@echo "Running desktop application..."
	cargo run

# Run in development mode with hot reload
dev:
	@echo "Running in development mode..."
	cargo watch -x run

# Serve web application
serve-web: build-web
	@echo "Starting web server..."
	@cd web && python3 -m http.server 8000

# Create release packages
release: clean build
	@echo "Creating release packages..."
	@mkdir -p releases
	@if [ -f "target/release/opensyncedlists" ]; then \
		tar -czf releases/opensyncedlists-desktop-linux-x64.tar.gz -C target/release opensyncedlists README.md; \
	fi
	@if [ -f "target/release/opensyncedlists.exe" ]; then \
		zip -j releases/opensyncedlists-desktop-windows-x64.zip target/release/opensyncedlists.exe README.md; \
	fi
	@if [ -d "pkg" ]; then \
		tar -czf releases/opensyncedlists-web.tar.gz pkg/ web/ README.md; \
	fi
	@echo "Release packages created in releases/"

# Show help
help:
	@echo "OpenSyncedLists Build System"
	@echo ""
	@echo "Available targets:"
	@echo "  build         - Build for all platforms"
	@echo "  build-desktop - Build desktop application"
	@echo "  build-web     - Build web application"
	@echo "  test          - Run tests"
	@echo "  clean         - Clean build artifacts"
	@echo "  install       - Install dependencies"
	@echo "  run           - Run desktop application"
	@echo "  dev           - Run in development mode"
	@echo "  serve-web     - Build and serve web application"
	@echo "  release       - Create release packages"
	@echo "  help          - Show this help"
