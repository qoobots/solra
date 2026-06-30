# ============================================================================
# Solra Core SDK - Build Targets
# ============================================================================
# Usage:
#   make ios          - Build for iOS arm64
#   make android      - Build for Android arm64-v8a
#   make windows      - Build for Windows x64 (MSVC)
#   make macos        - Build for macOS arm64
#   make all          - Build all platforms (on respective hosts)
#   make test         - Run unit tests (native platform)
#   make clean        - Clean all build artifacts
#   make lint         - Run clang-tidy and clang-format checks
#   make verify       - Full verification (lint + build + test)
# ============================================================================

BUILD_TYPE ?= Release
BUILD_JOBS ?= $(shell nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)
CORE_DIR := $(dir $(realpath $(lastword $(MAKEFILE_LIST))))

.PHONY: all ios android windows macos test clean lint verify help

help:
	@echo "Solra Core SDK Build Targets:"
	@echo "  make ios       - Build for iOS arm64 (macOS host only)"
	@echo "  make android   - Build for Android arm64-v8a"
	@echo "  make windows   - Build for Windows x64 (MSVC)"
	@echo "  make macos     - Build for macOS arm64"
	@echo "  make all       - Build for current host platform"
	@echo "  make test      - Run unit tests"
	@echo "  make clean     - Clean all build artifacts"
	@echo "  make lint      - Run clang-tidy + clang-format checks"
	@echo "  make verify    - Full verification: lint + build + test"

# ── Platform-specific builds ──

ios:
	bash $(CORE_DIR)/scripts/build-ios.sh

android:
	bash $(CORE_DIR)/scripts/build-android.sh

windows:
	bash $(CORE_DIR)/scripts/build-windows.sh

macos:
	bash $(CORE_DIR)/scripts/build-macos.sh

# ── Auto-detect platform ──

UNAME_S := $(shell uname -s)
UNAME_M := $(shell uname -m)

all:
ifeq ($(UNAME_S),Darwin)
	@echo "Building for macOS $(UNAME_M)..."
	@$(MAKE) macos
else ifeq ($(UNAME_S),Linux)
	@echo "Building for Linux (x64)..."
	@echo "Linux build: use cmake directly or run in CI"
else ifeq ($(OS),Windows_NT)
	@echo "Building for Windows x64..."
	@$(MAKE) windows
else
	@echo "Unknown platform: $(UNAME_S)"
	@exit 1
endif

# ── Test ──

test:
	@echo "Running unit tests..."
	@if [ -d "$(CORE_DIR)/build/macos/Debug" ]; then \
		cd $(CORE_DIR)/build/macos/Debug && ctest --output-on-failure; \
	elif [ -d "$(CORE_DIR)/build/windows/Debug" ]; then \
		cd $(CORE_DIR)/build/windows/Debug && ctest --output-on-failure -C Debug; \
	else \
		echo "No debug build found. Run 'make macos BUILD_TYPE=Debug' or 'make windows BUILD_TYPE=Debug' first."; \
		exit 1; \
	fi

# ── Lint ──

lint:
	@echo "Running clang-format check..."
	@find $(CORE_DIR)/src -name '*.cpp' -o -name '*.hpp' | xargs clang-format --dry-run --Werror
	@echo "Running clang-tidy..."
	@cmake -B $(CORE_DIR)/build/tidy -DCMAKE_EXPORT_COMPILE_COMMANDS=ON $(CORE_DIR)
	@run-clang-tidy -p $(CORE_DIR)/build/tidy \
		-checks='-*,bugprone-*,performance-*,modernize-*,cppcoreguidelines-*' \
		-header-filter='$(CORE_DIR)/src/.*' \
		$(CORE_DIR)/src/

# ── Full verification ──

verify: lint
	@$(MAKE) all BUILD_TYPE=Debug
	@$(MAKE) test

# ── Clean ──

clean:
	@echo "Cleaning build artifacts..."
	@rm -rf $(CORE_DIR)/build
	@echo "Clean complete."
