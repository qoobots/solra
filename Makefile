# Build all subprojects
build:
	@echo "Building all Solra subprojects..."

# Generate proto code for all languages
proto:
	@echo "Generating proto code..."
	@bash tools/codegen/generate-proto.sh

# Run all tests
test:
	@echo "Running all tests..."

# Start local development environment
dev:
	@echo "Starting local development environment..."
	@bash tools/dev-env/start-local.sh

# Stop local development environment
stop:
	@bash tools/dev-env/stop-local.sh

# Clean build artifacts
clean:
	@echo "Cleaning build artifacts..."

# Code formatting
format:
	@echo "Formatting code..."

# Lint checking
lint:
	@echo "Running lint checks..."

.PHONY: build proto test dev stop clean format lint
