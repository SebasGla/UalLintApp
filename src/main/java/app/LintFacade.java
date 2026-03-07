package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/*
 * A simple wrapper (facade) that bridges the gap between the core linter logic
 * and the JavaFX GUI. It translates the rich diagnostic data into flat rows
 * that can be easily displayed in the UI table.
 */
public final class LintFacade
{
    private LintFacade()
    {
        // Prevent instantiation, this is purely a utility class
    }

    /*
     * Executes the linter and converts the complex Diagnostic objects into simple
     * DiagnosticRow objects for the JavaFX TableView.
     */
    public static List<DiagnosticRow> run(String sourceText, String sourceName, Map<Long, String> addrToName, List<String> enabledRules)
    {
        // Pass all the raw inputs from the GUI directly into the main linter engine
        DiagnosticCollector dc = LinterRunner.run(sourceText, sourceName, addrToName, enabledRules);

        List<DiagnosticRow> rows = new ArrayList<>();

        // Loop through the sorted findings and flatten them into simple table rows
        for (Diagnostic d : dc.getAllSorted())
        {
            rows.add(
                    new DiagnosticRow(
                            d.getSourceName(),
                            d.getSeverity().name(),
                            d.getRuleId().name(),
                            d.getLine(),
                            d.getColumn(),
                            d.getMessage(),
                            d.getOffendingText()
                    )
            );
        }

        return rows;
    }
}