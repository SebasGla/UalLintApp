package app;

import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class DiagnosticCollector
{

    private final List<Diagnostic> diagnostics = new ArrayList<>();
    private final String sourceName; // optional: set to your file name/path

    public DiagnosticCollector(String sourceName)
    {
        this.sourceName = sourceName;
    }

    public void report(Rules ruleId, Severity severity, Token token, String message)
    {
        Objects.requireNonNull(token, "token");
        diagnostics.add(new Diagnostic(ruleId, severity, token.getLine(), token.getCharPositionInLine(), message, safeTokenText(token), sourceName));
    }

    public void reportAt(Rules ruleId, Severity severity, int line, int column, String message, String offendingText)
    {
        diagnostics.add(new Diagnostic(ruleId, severity, line, column, message, offendingText, sourceName));
    }

    public List<Diagnostic> getAllSorted()
    {
        List<Diagnostic> copy = new ArrayList<>(diagnostics);
        Collections.sort(copy);
        return copy;
    }

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

    public void printHumanReadable() {
        for (Diagnostic d : getAllSorted()) {
            System.out.println(d.toString());
        }
    }

    private static String safeTokenText(Token token) {
        // token.getText() may be null for some synthetic tokens
        String t = token.getText();
        return (t == null) ? "" : t;
    }
}
