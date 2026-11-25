#!/usr/bin/env bash
# Welche Teile haben sich seit dem vorherigen Release geändert? Verglichen wird der Tag mit dem vorherigen Release-Tag,
# nicht mit dem letzten Commit. Ausgabe im Format für $GITHUB_OUTPUT, zum Beispiel:
#   previous=v1.2.0
#   backend=false
#   frontend=true
# Aufruf: scripts/changes.sh v1.3.0 (braucht die ganze Git-Historie mit allen Tags)
set -euo pipefail

tag=$1

# Der neueste Release-Tag vor diesem Tag. Gibt es keinen, ist das der allererste Release: alles deployen
previous=$(git describe --tags --abbrev=0 --match 'v*' "$tag^" 2>/dev/null || true)
if [ -z "$previous" ]; then
  echo "previous="
  echo "backend=true"
  echo "frontend=true"
  exit 0
fi

files=$(git diff --name-only "$previous" "$tag")
backend=false
frontend=false

# Backend: alles, woraus das JAR entsteht. Code unter src/main und der Build mit Gradle
if grep -qE '^(src/main/|settings\.gradle$|gradle\.properties$|gradle/|gradlew)' <<< "$files"; then
  backend=true
fi

# build.gradle ändert sich bei jedem Release, weil dort die Versionsnummer steht. Die neue Nummer allein ist keine
# Änderung am Backend. Zählt nur, wenn sich in build.gradle noch eine andere Zeile geändert hat
if git diff -U0 "$previous" "$tag" -- build.gradle | grep -E '^[+-]' | grep -vE '^(\+\+\+|---) ' | grep -vqE "^[+-]version = '"; then
  backend=true
fi

# Frontend: der Ordner, den der Release-Workflow als ZIP packt
if grep -q '^frontend/' <<< "$files"; then
  frontend=true
fi

# Alles andere ändert am Spiel nichts: Tests, Dokumentation, Workflows, infra/, webkonsole/, DevContainer
echo "previous=$previous"
echo "backend=$backend"
echo "frontend=$frontend"
