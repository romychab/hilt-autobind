#!/usr/bin/env bash
# Parses the lib-compiler JaCoCo XML report and exports coverage variables:
#   COVERED  - number of covered instructions
#   MISSED   - number of missed instructions
#   TOTAL    - total instructions (COVERED + MISSED)
#   PCT      - coverage percentage rounded to 1 decimal (e.g. "87.3")
#   PCT_INT  - coverage percentage rounded to nearest integer (e.g. "87")
#
# Usage: source build-scripts/jacoco-coverage.sh

XML="lib-compiler/build/reports/jacoco/test/jacocoTestReport.xml"

if [ ! -f "$XML" ]; then
    echo "::error::Coverage report not found: $XML. Run :lib-compiler:jacocoTestReport first."
    exit 1
fi

read -r COVERED MISSED < <(python3 - "$XML" <<'EOF'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
ctr = next(
    (c for c in reversed(list(root.iter('counter'))) if c.get('type') == 'INSTRUCTION'),
    None,
)
if ctr is None:
    sys.exit("No INSTRUCTION counter found")
print(ctr.get('covered'), ctr.get('missed'))
EOF
)

TOTAL=$((COVERED + MISSED))

if [ "$TOTAL" -eq 0 ]; then
    echo "::error::No instruction coverage data found in $XML."
    exit 1
fi

PCT=$(awk "BEGIN { printf \"%.1f\", ($COVERED / $TOTAL) * 100 }")
PCT_INT=$(awk "BEGIN { printf \"%.0f\", ($COVERED / $TOTAL) * 100 }")
