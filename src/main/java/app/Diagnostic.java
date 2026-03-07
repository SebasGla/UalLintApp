package app;

import java.util.Objects;

/*
 * A data container representing a single linting finding (an error or warning).
 * Stores all the necessary context so we can print a helpful message to the user later.
 */
public final class Diagnostic implements Comparable<Diagnostic>
{
    private final Rules ruleId;         // Which rule was violated
    private final Severity severity;    // How critical the finding is (e.g., ERROR, WARNING)
    private final int line;             // 1-based line number where the issue occurred
    private final int column;           // 0-based column index (standard ANTLR format)
    private final String message;       // Human-readable explanation of the issue
    private final String offendingText; // The exact snippet of code that triggered the rule (optional)
    private final String sourceName;    // The file name being analyzed (optional)

    /*
     * Creates a new diagnostic record.
     * Enforces strict null-checks on essential fields so the app doesn't crash later during formatting.
     */
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

    /*
     * Defines the sorting order for diagnostics when we print the final report.
     * Sorts primarily by file location (line, then column) so errors read top-to-bottom.
     */
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

        // Fallback sorting: if issues are at the exact same location, sort by severity and rule type
        c = this.severity.compareTo(other.severity);

        if (c != 0)
            return c;

        c = this.ruleId.compareTo(other.ruleId);

        if (c != 0)
            return c;

        return this.message.compareTo(other.message);
    }

    /*
     * Formats the diagnostic into a clean, console-ready string.
     * Handles optional fields (like sourceName and offendingText) gracefully to prevent printing "null".
     */
    @Override
    public String toString()
    {
        // Only append the filename prefix if we actually have one
        String src = (sourceName == null || sourceName.isBlank()) ? "" : (sourceName + ":");

        // Only append the code snippet if we successfully captured it
        String off = (offendingText == null || offendingText.isBlank()) ? "" : (" Offending: `" + offendingText + "`");

        // Example output: "main.s:10:5 [WARNING] AbsoluteAddress: Absolute address used... Offending: `0x400`"
        return String.format("%s%d:%d [%s] %s: %s%s", src, line, column, severity, ruleId, message, off);
    }
}