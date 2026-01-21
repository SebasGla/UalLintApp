package app;

import java.util.Objects;

/*
    Diagnostic class for characterising Lint findings
 */
public final class Diagnostic implements Comparable<Diagnostic>
{
    private final Rules ruleId; //rule Id according to Rule List
    private final Severity severity;
    private final int line;          // 1-based
    private final int column;        // 0-based (ANTLR convention)
    private final String message;
    private final String offendingText; // may be null
    private final String sourceName;    // may be null (e.g., file path)

    public Diagnostic(Rules ruleId, Severity severity, int line, int column, String message, String offendingText, String sourceName)
    {
        this.ruleId = Objects.requireNonNull(ruleId, "ruleId");
        this.severity = Objects.requireNonNull(severity, "severity");
        this.line = line;
        this.column = column;
        this.message = Objects.requireNonNull(message, "message");
        this.offendingText = offendingText;
        this.sourceName = sourceName;
    }

    //getter Methods
    public Rules getRuleId()
    {
        return ruleId;
    }
    public Severity getSeverity()
    {
        return severity;
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
    public String getSourceName()
    {
        return sourceName;
    }

    @Override
    public int compareTo(Diagnostic other)
    {
        int c;

        c = Integer.compare(this.line, other.line);

        if (c != 0)
            return c;

        c = Integer.compare(this.column, other.column);

        if (c != 0)
            return c;

        c = this.severity.compareTo(other.severity);

        if (c != 0)
            return c;

        c = this.ruleId.compareTo(other.ruleId);

        if (c != 0)
            return c;

        return this.message.compareTo(other.message);
    }

    @Override
    public String toString()
    {
        String src = (sourceName == null || sourceName.isBlank()) ? "" : (sourceName + ":");
        String off = (offendingText == null || offendingText.isBlank()) ? "" : (" Offending: `" + offendingText + "`");
        return String.format("%s%d:%d [%s] %s: %s%s", src, line, column, severity, ruleId, message, off);
    }
}
