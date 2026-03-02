package app;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import antlr4gen.*;


public final class AbsoluteAddressLintListener extends LinterParserBaseListener
{
    private final DiagnosticCollector diags;

    public AbsoluteAddressLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    @Override
    public void exitLiteral(LinterParser.LiteralContext ctx)
    {
        if (ctx.constExpr() == null)
            return;

        if (isHexOnlyConstExpr(ctx.constExpr()))
        {
            Token t = ctx.constExpr().getStart();
            diags.report(Rules.AbsoluteAddress, Severity.WARNING, t, "Absolute address used; use a label/symbol instead of a magic address.");
        }
    }

    private static boolean isHexOnlyConstExpr(LinterParser.ConstExprContext ctx)
    {
        if (containsTokenType(ctx, LinterLexer.ID))
            return false;

        return containsTokenType(ctx, LinterLexer.INT_HEX);
    }

    private static boolean containsTokenType(ParseTree node, int tokenType)
    {
        if (node == null)
            return false;

        if (node instanceof TerminalNode)
        {
            TerminalNode t = (TerminalNode) node;

            if (t.getSymbol() == null)
                return false;

            if (t.getSymbol().getType() == tokenType)
                return true;

            return false;
        }

        int n = node.getChildCount();
        for (int i = 0; i < n; i++)
        {
            if (containsTokenType(node.getChild(i), tokenType))
                return true;
        }

        return false;
    }
}