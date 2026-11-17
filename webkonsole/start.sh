#!/usr/bin/env bash
# Startet TicTacTest im Browser, mit dem JAR aus dem Release und ohne lokalen Build.
#
#   webkonsole/start.sh v1.1.0
#
# Danach: http://localhost:8080 ist das Terminal, http://localhost:8080/version.txt die Version.
# Beenden mit Ctrl+C.
set -euo pipefail

version=${1#v}                        # Release-Tag ohne "v", zum Beispiel 1.1.0
dir=/tmp/tictactest                   # Arbeitsordner: JAR, version.txt und die Dateien von nginx
conf="$(cd "$(dirname "$0")" && pwd)/nginx.conf"
jar="$dir/tictactest-$version.jar"

mkdir -p "$dir"

# 1. Das JAR aus dem Release holen, nicht selbst bauen
if [ ! -f "$jar" ]; then
	curl -fsSL -o "$jar" "https://github.com/Flashbibi/450-tictactest-mvk/releases/download/v$version/tictactest-$version.jar"
fi

# 2. Die Version kommt aus dem Release-Tag, niemand pflegt sie von Hand
echo "$version" > "$dir/version.txt"

# 3. nginx auf Port 8080: Terminal und /version.txt. Endet das Skript, wird nginx auch gestoppt
nginx -e stderr -p "$dir" -c "$conf"
trap 'nginx -e stderr -p "$dir" -c "$conf" -s stop' EXIT

# 4. ttyd startet für jede Verbindung einen eigenen Java-Prozess mit dem JAR, nie eine Shell.
#    --interface 127.0.0.1: ttyd ist nur über nginx erreichbar
#    --writable:            Eingaben aus dem Browser erlaubt, sonst kann man nur zuschauen
#    --check-origin:        WebSocket nur von der eigenen Seite, keine fremde Webseite kann ein Spiel starten
#    --max-clients 10:      höchstens 10 Spiele gleichzeitig, jedes ist eine eigene JVM mit rund 45 MB
#    -Xmx32m:               ein einzelnes Spiel kann nicht mehr als 32 MB Heap belegen
echo "TicTacTest $version: http://localhost:8080"
ttyd --interface 127.0.0.1 --port 7681 --writable --check-origin --max-clients 10 \
	java -Xmx32m -jar "$jar"
