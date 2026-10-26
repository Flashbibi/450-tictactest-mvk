package ch.bbw.m450.tictactoe;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

/**
 * Zusätzliches Testing-Framework: ArchUnit prüft Regeln über die Abhängigkeiten zwischen Klassen. Es liest den
 * kompilierten Produktionscode (ohne Tests) und läuft über die JUnit Platform, also automatisch mit
 * {@code ./gradlew test}.
 */
@AnalyzeClasses(packages = "ch.bbw.m450.tictactoe", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

	@ArchTest
	static final ArchRule PLAYERS_DO_NOT_KNOW_EACH_OTHER = noClasses()
			.that().resideInAPackage("..players..")
			.should().dependOnClassesThat().resideInAPackage("..players..")
			.because("ein Spieler spricht nur über TicTacToePlayer und TicTacToeMain mit dem Spiel");
}
