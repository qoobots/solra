#!/bin/bash
# Avro Schema Compatibility Checker
# Usage: ./check.sh [old_version] [new_version]
set -euo pipefail

OLD_VERSION="${1:-v1}"
NEW_VERSION="${2:-v2}"
AVRO_DIR="$(cd "$(dirname "$0")/.." && pwd)"

echo "=== Avro Schema Compatibility Check ==="
echo "Old version: $OLD_VERSION"
echo "New version: $NEW_VERSION"
echo ""

TOTAL=0
PASS=0
FAIL=0

# Check if avro-tools is available
if ! command -v avro-tools &>/dev/null && ! java -jar avro-tools.jar 2>/dev/null; then
    echo "[WARN] avro-tools not found, falling back to structural diff"
    AVRO_MODE="diff"
else
    AVRO_MODE="compat"
fi

for new_schema in "$AVRO_DIR"/"$NEW_VERSION"/**/*.avsc; do
    event_name=$(basename "$new_schema" .avsc)
    
    # Find matching old schema
    old_schema=$(find "$AVRO_DIR/$OLD_VERSION" -name "${event_name}.avsc" 2>/dev/null | head -1)
    
    TOTAL=$((TOTAL + 1))
    
    if [ -z "$old_schema" ]; then
        echo "  [NEW] $event_name - no compatibility check needed"
        PASS=$((PASS + 1))
        continue
    fi

    echo -n "  $event_name: "
    
    if [ "$AVRO_MODE" = "compat" ]; then
        if java -jar avro-tools.jar compatibility "$new_schema" "$old_schema" BACKWARD 2>/dev/null; then
            echo "BACKWARD ✅"
        else
            echo "BACKWARD ❌ BREAKING CHANGE DETECTED"
            FAIL=$((FAIL + 1))
            continue
        fi
        
        if java -jar avro-tools.jar compatibility "$new_schema" "$old_schema" FORWARD 2>/dev/null; then
            echo "               FORWARD ✅"
            PASS=$((PASS + 1))
        else
            echo "               FORWARD ❌"
            FAIL=$((FAIL + 1))
        fi
    else
        # Fallback: compare field names
        old_fields=$(grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' "$old_schema" | sort)
        new_fields=$(grep -o '"name"[[:space:]]*:[[:space:]]*"[^"]*"' "$new_schema" | sort)
        removed=$(comm -23 <(echo "$old_fields") <(echo "$new_fields") | wc -l)
        if [ "$removed" -gt 0 ]; then
            echo "❌ $removed field(s) removed"
            FAIL=$((FAIL + 1))
        else
            echo "✅"
            PASS=$((PASS + 1))
        fi
    fi
done

echo ""
echo "=== Results ==="
echo "Total: $TOTAL, Pass: $PASS, Fail: $FAIL"

if [ "$FAIL" -gt 0 ]; then
    echo "❌ Compatibility check FAILED"
    exit 1
else
    echo "✅ All checks passed"
    exit 0
fi
