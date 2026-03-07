package app;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/*
 * The central hub for gathering linting results.
 * Listeners report their findings here, and this class stores, sorts,
 * and formats them for the user.
 */
public final class DiagnosticCollector
{
    // The internal list where all warnings and errors are stored during the run
    private final List<Diagnostic> diagnostics = new ArrayList<>();

    // Tracks the current file being analyzed so errors can point to the right file
    private final String sourceName;

    public DiagnosticCollector(String sourceName)
    {
        this.sourceName = sourceName;
    }

    /*
     * Records a new finding using an ANTLR Token.
     * This is the preferred method because it automatically extracts the exact
     * line, column, and text snippet right out of the token.
     */
    public void report(Rules ruleId, Severity severity, Token token, String message)
    {
        Objects.requireNonNull(token, "token");
        diagnostics.add(new Diagnostic(ruleId, severity, token.getLine(), token.getCharPositionInLine(), message, safeTokenText(token), sourceName));
    }

    /*
     * Records a finding using manual line and column numbers.
     * Useful for file-wide errors (like BlankEof) that aren't tied to one specific token.
     */
    public void reportAt(Rules ruleId, Severity severity, int line, int column, String message, String offendingText)
    {
        diagnostics.add(new Diagnostic(ruleId, severity, line, column, message, offendingText, sourceName));
    }

    /*
     * Returns a sorted copy of all findings.
     * Sorting ensures the final output reads logically from the top of the file to the bottom.
     */
    public List<Diagnostic> getAllSorted()
    {
        List<Diagnostic> copy = new ArrayList<>(diagnostics);
        Collections.sort(copy);
        return copy;
    }

    /*
     * Checks if any critical errors were found.
     * Super useful for CI/CD pipelines to determine if the build should fail.
     */
    public boolean hasErrors()
    {
        return diagnostics.stream().anyMatch(d -> d.getSeverity() == Severity.ERROR);
    }

    public int size()
    {
        return diagnostics.size();
    }

    public void clear() {
        diagnostics.clear();
    }

    /*
     * Dumps all sorted diagnostics directly to the console.
     */
    public void printHumanReadable() {
        for (Diagnostic d : getAllSorted()) {
            System.out.println(d.toString());
        }
    }

    /*
     * Helper to safely extract text from a token.
     * ANTLR sometimes generates "synthetic" hidden tokens that don't have actual text,
     * so this prevents random NullPointerExceptions.
     */
    private static String safeTokenText(Token token) {
        String t = token.getText();
        return (t == null) ? "" : t;
    }
}