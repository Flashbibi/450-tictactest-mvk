// Bettet das Terminal der Web-Konsole ein und zeigt die Versionen von Frontend und Backend.
// Die Adresse der Web-Konsole kommt aus config.js, nicht aus diesem Code.
const webConsoleUrl = window.TICTACTEST_CONFIG.webConsoleUrl;

document.getElementById("terminal").src = webConsoleUrl;
document.getElementById("terminal-link").href = webConsoleUrl;

showVersion("frontend-version", "version.txt");                          // liegt neben dieser Seite
showVersion("backend-version", new URL("version.txt", webConsoleUrl));   // liefert nginx vor der Web-Konsole

async function showVersion(elementId, url) {
	const element = document.getElementById(elementId);
	try {
		const response = await fetch(url, { cache: "no-store" });
		if (!response.ok) {
			throw new Error("HTTP " + response.status);
		}
		element.textContent = (await response.text()).trim();
	} catch (error) {
		element.textContent = "nicht erreichbar";
		console.error("Version von " + url + " nicht lesbar:", error);
	}
}
