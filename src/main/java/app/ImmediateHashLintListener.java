package app;

import org.antlr.v4.runtime.Token;
import antlr4gen.*;

public final class ImmediateHashLintListener extends LinterParserBaseListener
{
    private final DiagnosticCollector diags;

    public ImmediateHashLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    @Override
    public void exitImmediateMissingHash(LinterParser.ImmediateMissingHashContext ctx)
    {
        Token t = ctx.getStart();

        diags.report(Rules.IMMEDIATE, Severity.ERROR, t, "Immediate value without '#' sigil.");
    }
}
