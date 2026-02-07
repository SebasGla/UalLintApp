package app;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class LintFacade
{
    private LintFacade()
    {
    }

    public static List<DiagnosticRow> run(String sourceText, String sourceName, Map<Long, String> addrToName)
    {
        DiagnosticCollector dc = LinterRunner.run(sourceText, sourceName, addrToName);

        List<DiagnosticRow> rows = new ArrayList<>();

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
