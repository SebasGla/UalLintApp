package app;

import org.antlr.v4.runtime.Token;
import antlr4gen.*;

/*
 * Linting rule to enforce the use of the '#' symbol before immediate values.
 * In UAL, raw numbers should be prefixed with '#' (e.g., #4 instead of just 4)
 * to clearly distinguish them from registers or memory addresses.
 */
public final class ImmediateHashLintListener extends LinterParserBaseListener
{
    private final DiagnosticCollector diags;

    public ImmediateHashLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    /*
     * Hook that triggers whenever the parser finds an immediate value missing its hash.
     * The ANTLR grammar actually separates valid (with hash) and invalid (without hash)
     * immediates into different rules, which makes catching this error incredibly simple.
     */
    @Override
    public void exitImmediateMissingHash(LinterParser.ImmediateMissingHashContext ctx)
    {
        // Grab the exact token where the number starts so we can point the error to it
        Token t = ctx.getStart();

        diags.report(Rules.ImmediateHash, Severity.ERROR, t, "Immediate value without '#' character.");
    }
}