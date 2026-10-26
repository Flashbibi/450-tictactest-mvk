#!/usr/bin/env bash
# Liest einen Coverage-Wert aus dem JaCoCo-XML-Report und gibt ihn in Prozent aus, z.B. 53.8
# Aufruf: scripts/coverage.sh build/reports/jacoco/test/jacocoTestReport.xml INSTRUCTION
set -euo pipefail

report=$1
type=$2 # INSTRUCTION, BRANCH, LINE, ...

# Die Gesamtwerte des Projekts stehen im letzten <counter> dieses Typs, z.B.
#   <counter type="BRANCH" missed="36" covered="42"/>
counter=$(grep -o "<counter type=\"$type\"[^>]*>" "$report" | tail -n 1)

missed=$(echo "$counter" | grep -o 'missed="[0-9]*"' | grep -o '[0-9]\+')
covered=$(echo "$counter" | grep -o 'covered="[0-9]*"' | grep -o '[0-9]\+')

# covered / (covered + missed) × 100, auf eine Dezimalstelle gerundet
awk -v covered="$covered" -v missed="$missed" 'BEGIN { printf "%.1f\n", covered * 100 / (covered + missed) }'
