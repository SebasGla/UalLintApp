package app;

/*
 * A flat data container representing a single row of a linting report.
 * This is primarily used for exporting the results into tabular formats
 * like CSV files or displaying them cleanly in a UI grid.
 */
public final class DiagnosticRow
{
    private final String sourceName;    // The file where the issue was found
    private final String severity;      // E.g., "ERROR" or "WARNING"
    private final String ruleId;        // The specific rule that was triggered
    private final int line;             // 1-based line number
    private final int column;           // 0-based column index
    private final String message;       // The human-readable warning/error text
    private final String offendingText; // The exact code snippet that caused the issue

    /*
     * Creates a complete snapshot of a linting issue for a single table row.
     */
    public DiagnosticRow(String sourceName, String severity, String ruleId, int line, int column, String message, String offendingText)
    {
        this.sourceName = sourceName;
        this.severity = severity;
        this.ruleId = ruleId;
        this.line = line;
        this.column = column;
        this.message = message;
        this.offendingText = offendingText;
    }

    public String getSourceName()
    {
        return sourceName;
    }

    public String getSeverity()
    {
        return severity;
    }

    public String getRuleId()
    {
        return ruleId;
    }

    public int getLine()
    {
        return line;
    }

    public int getColumn()
    {
        return column;
    }

    public String getMessage()
    {
        return message;
    }

    public String getOffendingText()
    {
        return offendingText;
    }
}