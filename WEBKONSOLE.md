# Web-Konsole: TicTacTest im Browser

M324 Webapp, Auftrag 1 · Stand: 17.11.2026

Das Spiel bleibt eine Konsolen-Applikation und wird nicht verändert. Eine Web-Konsole startet das JAR aus dem Release für jeden Besucher und überträgt Ein- und Ausgabe in ein Terminal im Browser.

## Aufbau

```
Browser
 ├─ http://localhost:8000              Frontend: statische Dateien aus frontend/
 │    ├─ iframe ──► http://localhost:8080/             nginx ──► ttyd (127.0.0.1:7681) ──► java -jar tictactest-<Version>.jar
 │    └─ fetch  ──► http://localhost:8080/version.txt  nginx, mit CORS-Header
```

- **ttyd** startet für jede Browser-Verbindung einen eigenen Java-Prozess in einem Pseudo-Terminal (PTY). Das Spiel merkt nicht, dass es im Browser läuft: Es liest von der Tastatur und schreibt ins Terminal wie immer.
- Zwischen Browser und ttyd läuft eine **WebSocket**-Verbindung. Sie bleibt offen und überträgt die Tastendrücke zum Spiel und die Ausgabe zurück.
- Im Browser zeichnet **xterm.js** das Terminal, inklusive der ANSI-Farben. xterm.js ist in ttyd schon eingebaut.
- **nginx** ist die einzige Adresse nach aussen (Port 8080). Es leitet das Terminal an ttyd weiter und liefert die Version aus.

## Werkzeug: ttyd

| Werkzeug | Bewertung |
|---|---|
| **ttyd** (gewählt) | echtes Terminal mit PTY, xterm.js eingebaut, ANSI-Farben funktionieren ohne Anpassung. Aktiv gepflegt (Version 1.7.7) und als Alpine-Paket verfügbar: `apk add ttyd` |
| GoTTY | funktioniert ähnlich, wird aber kaum noch gepflegt |
| websocketd | überträgt nur Textzeilen und ist kein Terminal. Es bräuchte eine eigene HTML/JS-Seite, und die ANSI-Farbcodes müssten wir selbst umwandeln |

## Lokal starten

Im DevContainer, ab Image `v1.0.2`. Dort sind ttyd, nginx und curl installiert, und VS Code leitet die Ports 8080 und 8000 weiter.

```bash
webkonsole/start.sh v1.0.2        # holt das JAR aus dem Release v1.0.2 und startet nginx und ttyd
```

Danach ist das Terminal auf <http://localhost:8080>. Für das Frontend in einem zweiten Terminal:

```bash
"$JAVA_HOME/bin/jwebserver" -p 8000 -d "$PWD/frontend"
```

Das Frontend liegt auf <http://localhost:8000>. `jwebserver` ist ein einfacher Webserver für statische Dateien, den das JDK mitbringt. Beenden jeweils mit Ctrl+C. Beim Beenden von `start.sh` wird auch nginx gestoppt.

## Was `start.sh` macht

1. Es lädt `tictactest-<Version>.jar` aus dem GitHub-Release nach `/tmp/tictactest`. Gebaut wird nichts.
2. Es schreibt die Version in `/tmp/tictactest/version.txt`.
3. Es startet nginx mit [`webkonsole/nginx.conf`](webkonsole/nginx.conf). Weil nginx als normaler Benutzer läuft, liegen alle Dateien von nginx im Arbeitsordner (Option `-p`).
4. Es startet ttyd im Vordergrund:

```bash
ttyd --interface 127.0.0.1 --port 7681 --writable --check-origin --max-clients 10 \
	java -Xmx32m -jar /tmp/tictactest/tictactest-<Version>.jar
```

| Option | Warum |
|---|---|
| `java -Xmx32m -jar …` | der einzige Befehl, den ttyd startet: das JAR, keine Shell. `-Xmx32m` begrenzt den Heap eines Spiels |
| `--writable` | In ttyd 1.7 ist das Terminal standardmässig nur zum Zuschauen. Ohne die Option erscheint eine getippte Zahl gar nicht (getestet) |
| `--interface 127.0.0.1` | ttyd ist nur von der Maschine selbst erreichbar, von aussen nur über nginx |
| `--check-origin` | Die WebSocket-Verbindung muss von der eigenen Seite kommen. Darum gibt nginx den Host mit Port weiter (`$http_host`). Mit `$host` fehlt der Port, und ttyd lehnt jede Verbindung ab (getestet: 502) |
| `--max-clients 10` | höchstens 10 Spiele gleichzeitig, siehe Speicher unten |

## Konfiguration

| Was | Wo | Lokal |
|---|---|---|
| Adresse der Web-Konsole für das Frontend | `frontend/config.js` (`webConsoleUrl`) | `http://localhost:8080/` |
| Port nach aussen | `webkonsole/nginx.conf` (`listen`) | 8080 |
| Port von ttyd, nur intern | `webkonsole/start.sh` und `nginx.conf` (`proxy_pass`) | 7681 |
| Version des Backends | Release-Tag, Argument von `start.sh` | z. B. `v1.0.2` |

`index.html` lädt `config.js` vor `app.js`. Für eine andere Umgebung, zum Beispiel den Server in Auftrag 2, wird beim Deployment nur `config.js` ersetzt. Der Code bleibt gleich.

## Versionsabfrage

```bash
curl http://localhost:8080/version.txt
1.0.2
```

- **Herkunft:** Die Version kommt aus dem Release-Tag. `start.sh v1.0.2` lädt genau das JAR dieses Releases und schreibt `1.0.2` in `version.txt`. Von Hand pflegt sie niemand. Dass Tag, Version in `build.gradle` und Name des JARs zusammenpassen, prüft schon der Release-Workflow.
- **Warum nicht aus dem Manifest:** Das Manifest unserer JARs enthält nur die `Main-Class`, keine Version. Das JAR wollen wir nicht verändern.
- **CORS:** nginx liefert `version.txt` mit `Access-Control-Allow-Origin: *` aus. Das Frontend liegt auf einem anderen Port, also auf einem anderen Origin. Ohne den Header blockiert der Browser das Lesen (siehe unten).
- **Frontend-Version:** Im Repository steht in `frontend/version.txt` „dev“. Der Release-Workflow schreibt dort die Version des Tags hinein, bevor er das ZIP packt.

## Untersuchungen

Getestet am 17.11.2026 mit ttyd 1.7.7 und dem JAR aus Release 1.0.2.

| Frage | Beobachtung |
|---|---|
| Tippen im Browser | geht nur mit `--writable`. Ohne die Option erscheint die Eingabe nicht, und das Spiel wartet weiter |
| Spielende | Das Spiel zeigt „…and the winner is: CROSS“, der Java-Prozess endet, ttyd schliesst die Verbindung. Im Browser erscheint „Press ⏎ to Reconnect“ |
| Neues Spiel | Enter drücken oder die Seite neu laden. Das öffnet eine neue Verbindung, ttyd startet einen neuen Java-Prozess |
| Ungültige Eingabe | Text wie „vier“ zeigt eine `NumberFormatException` mit Stacktrace im Browser, ein besetztes Feld oder Feld 9 eine `IllegalStateException`. Das Spiel endet in beiden Fällen, Enter startet ein neues. Das ist Fehler F2 aus M450 (siehe [`TESTKONZEPT.md`](TESTKONZEPT.md)) |
| Zwei Tabs | zwei Java-Prozesse, für jeden Tab einer. Ein Prozess braucht rund 45 MB (RSS). Einen Teil davon teilen sich die Prozesse, anteilig gerechnet (PSS) sind es rund 30 MB. Schliesst man einen Tab, beendet ttyd den Prozess (SIGHUP) |
| Server mit 1 GB RAM | Betriebssystem, nginx und ttyd brauchen geschätzt rund 200 MB. Es bleiben rund 800 MB, das reicht für etwa 17 Spiele zu 45 MB. Deshalb `--max-clients 10`, mit Reserve. Der elfte Tab bekommt keine Verbindung (nginx meldet 502), das Terminal bleibt leer. Ohne Grenze startet jeder neue Tab eine weitere JVM, bis der Speicher ausgeht (Denial of Service) |
| Einbetten im Frontend | Das `<iframe>` funktioniert, ttyd sendet keinen Header, der das Einbetten verbietet |
| Version im Frontend ohne CORS-Header | Die Entwickler-Konsole meldet: *Access to fetch at 'http://localhost:8080/…' from origin 'http://localhost:8000' has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header is present on the requested resource.* Mit dem Header in `nginx.conf` wird die Version angezeigt |

## Sicherheit

Eine Web-Konsole startet für jeden, der die URL kennt, ein Programm auf dem Server. Was das für ein Programm ist, entscheidet allein der Befehl hinter `ttyd`.

**Falsch konfiguriert,** zum Beispiel mit `ttyd --writable bash`, hätte jeder Besucher eine Shell auf dem Server, ohne Passwort und mit den Rechten des Benutzers. Er könnte Dateien lesen und löschen, auf einem EC2-Server über den Metadata-Dienst Zugangsdaten abholen, falls der Instanz eine Rolle zugewiesen ist, einen Krypto-Miner installieren oder von dort aus andere Systeme angreifen.

| Massnahme | Wirkung |
|---|---|
| ttyd startet direkt `java … -jar`, kein `bash -c` und kein Skript als Zwischenschritt | Endet das Spiel, endet die Verbindung. Es bleibt nie eine Shell offen |
| keine Option `--url-arg` | Besucher können keine Argumente an den Befehl anhängen |
| nginx und ttyd laufen als normaler Benutzer, nie als root | Selbst ein Fehler hätte nur die Rechte dieses Benutzers |
| ttyd nur auf `127.0.0.1` | Von aussen ist nur nginx erreichbar |
| `--check-origin` | Fremde Webseiten können kein Spiel öffnen. Getestet: Eine WebSocket-Verbindung, die eine Seite auf `localhost:8000` zur Web-Konsole öffnet, wird abgelehnt |
| `--max-clients 10` und `-Xmx32m` | begrenzen den Speicher gegen Denial of Service |
| Das Spiel liest nur Zahlen | Jede andere Eingabe beendet das Spiel. Ausgeführt wird nichts davon |

Für den Betrieb auf einem Server (Auftrag 2) kommen dazu: HTTPS vor nginx und eine Firewall, die nur die Ports 80 und 443 öffnet.

## Release

Ein Tag `v*` erzeugt den GitHub-Release mit zwei Dateien: `tictactest-<Version>.jar` und `tictactest-frontend-<Version>.zip`. `start.sh` und `nginx.conf` gehören zur Infrastruktur und liegen deshalb im Repository, nicht im Release. Die Begründung und den Test nur aus den Release-Assets beschreibt [`RELEASE.md`](RELEASE.md).

## Bekannte Einschränkungen

- Eine ungültige Eingabe beendet das Spiel und zeigt den Stacktrace (F2). Mit der Web-Konsole sehen das jetzt alle Spieler. Das ist ein guter Kandidat für einen Release, der nur das Backend ändert (Auftrag 3).
- `config.js` im Frontend-Archiv zeigt auf `localhost`. In der Cloud ersetzt das Deployment diese Datei.
