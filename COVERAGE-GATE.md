# Coverage Gate für Pull Requests

Auftrag 3 · Stand: 26.10.2026 · Workflow: [`.github/workflows/coverage-gate.yml`](.github/workflows/coverage-gate.yml)

## Regel

Ein Branch darf keine tiefere Test Coverage haben als der aktuelle `main`-Branch. Gleich viel ist erlaubt.

Verglichen wird die **Instruction Coverage**, dieselbe Metrik wie in der Time-Series (siehe [`COVERAGE-DESIGN.md`](COVERAGE-DESIGN.md), Frage 2).

## Wann läuft der Check?

- beim Öffnen eines Pull Requests und bei jedem weiteren Push darauf (`pull_request`)
- von Hand: *Actions → coverage-gate → Run workflow* (`workflow_dispatch`)

## Ablauf

1. Den Branch auschecken, `./gradlew test` ausführen und den Wert mit `scripts/coverage.sh` aus dem JaCoCo-XML lesen.
2. Den Wert von `main` aus der letzten Zeile von `coverage.csv` auf `gh-pages` lesen. So war es im Design unter „Ausblick auf Auftrag 3" geplant: `main` muss nicht ein zweites Mal getestet werden, weil der Job `coverage-history` den Wert nach jedem grünen Push auf `main` schon speichert.
3. Beide Werte vergleichen. Das Ergebnis steht im Log und in der Zusammenfassung des Laufs.
4. Bei einem Pull Request das Ergebnis zusätzlich als Kommentar in den PR schreiben (Bonus). Bei weiteren Pushes wird derselbe Kommentar aktualisiert.
5. Bei FAIL schlägt der letzte Schritt fehl, der Check wird rot.

## Ausgabe

```
Test Coverage (Instruction)

main:        45.6 %
Branch:      91.5 %
Änderung:    +45.9 Prozentpunkte

PASS - Coverage Gate bestanden
```

## Grenzen

- Der Wert von `main` stammt vom letzten grünen Lauf auf `main`. Läuft der Check, während `coverage-history` nach einem Merge noch arbeitet, vergleicht er mit dem Stand davor.
- Ein roter Check verhindert den Merge nur, wenn eine Branch-Protection-Regel ihn verlangt. Das ist in den Repository-Einstellungen nicht eingerichtet.
