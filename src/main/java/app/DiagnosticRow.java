package app;

public final class DiagnosticRow
{
    private final String sourceName;
    private final String severity;
    private final String ruleId;
    private final int line;
    private final int column;
    private final String message;
    private final String offendingText;

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
