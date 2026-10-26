# DevContainer: Versionierung und Freigabe

Stand: 26.10.2026 · Image: `ghcr.io/flashbibi/450-tictactest-mvk`

Dieses Dokument beschreibt den Ablauf. Die Entscheidungen dahinter stehen in [`DEVCONTAINER-KONZEPT.md`](DEVCONTAINER-KONZEPT.md).

## Was liegt wo

| Datei | Zweck |
|---|---|
| `.devcontainer/Dockerfile` | das Image: Alpine, Azul Zulu JDK 25, bash, git, User `dev` (UID:GID 1000:1000) |
| `.devcontainer/devcontainer.json` | lokale Entwicklungsumgebung für VS Code, zieht eine feste Image-Version aus der GHCR |
| `.github/workflows/devcontainer-release.yml` | baut und pusht das Image, erstellt den PR mit der neuen Version |
| `.github/workflows/build.yml` | die CI, läuft im DevContainer |

Gradle kommt über den Wrapper (`./gradlew`), JUnit und AssertJ über `build.gradle`. Beides muss nicht ins Image.

## Eine neue Version freigeben

1. Auf einem Branch `.devcontainer/Dockerfile` ändern, PR nach `main` erstellen und mergen.
2. Der Workflow `devcontainer-release` startet automatisch. Er baut das Image und pusht es als `v1.0.<Laufnummer>`, zum Beispiel `v1.0.1`.
3. Danach erstellt er den PR **„DevContainer v1.0.1 verwenden"**. Der PR setzt die neue Version in `devcontainer.json` und in allen Workflows ein.
4. Die CI dieses PRs läuft schon im neuen Image. Ist sie grün, den PR mergen. Das ist die Freigabe: Ab jetzt nutzen alle CI-Jobs die neue Version, und VS Code bietet beim nächsten Öffnen *Rebuild Container* an.

Branches bauen kein Image. Nur ein Merge nach `main` kann eine neue Version erzeugen.

Von Hand geht es auch: *Actions → devcontainer-release → Run workflow* baut das Dockerfile von `main` neu, zum Beispiel nach einem Update des Base-Images, und erstellt ebenfalls den PR.

## CI

Beide Jobs in `build.yml` laufen mit `container: image: ghcr.io/flashbibi/450-tictactest-mvk:v1.0.x`. Der Login erfolgt mit `GITHUB_TOKEN` und `packages: read`. Weil der Runner den Workspace als root anlegt, läuft der Container dort mit `options: --user root`. Der Default-User `dev` im Image bleibt davon unberührt.

## Lokal

`devcontainer.json` enthält `"image": "ghcr.io/…:v1.0.x"`. VS Code zieht genau diese Version. Das Package ist öffentlich, ein `docker login` ist nicht nötig.

Mit `latest` würde VS Code das lokal gespeicherte Image weiterverwenden und nie von selbst ein neues holen. Eine geänderte Versionsnummer dagegen erzwingt den Pull.

## Einmalige Einrichtung: Secret für den PR

Der Workflow ändert Dateien unter `.github/workflows/`. Das darf `GITHUB_TOKEN` nicht. Ausserdem starten PRs, die mit `GITHUB_TOKEN` erstellt werden, keine CI. Deshalb braucht es ein eigenes Token:

1. GitHub → *Settings → Developer settings → Personal access tokens → Fine-grained tokens → Generate new token*, nur für dieses Repository
2. Permissions, jeweils *Read and write*:

   | Permission | Wofür |
   |---|---|
   | Contents | Branch mit der neuen Version pushen |
   | Pull requests | PR erstellen |
   | Workflows | Dateien unter `.github/workflows/` ändern |

3. Im Repository: *Settings → Secrets and variables → Actions → New repository secret*, Name `DEVCONTAINER_PR_TOKEN`

Ohne das Secret bricht der Workflow gleich beim ersten Schritt ab.

## Historie

| Version | Datum | Inhalt |
|---|---|---|
| `v1.0.0` | 16.09.2026 | Erste Version, von Hand gepusht. Alpine 3.24, Azul Zulu 25, User `dev` |
