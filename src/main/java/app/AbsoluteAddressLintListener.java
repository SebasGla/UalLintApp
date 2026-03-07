package app;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import antlr4gen.*;

/*
 * Listener that walks the parse tree to find hardcoded (magic) memory addresses.
 * It warns the user to use symbols or labels instead of raw hex values.
 */
public final class AbsoluteAddressLintListener extends LinterParserBaseListener
{
    // Collects and stores all the linting warnings we find
    private final DiagnosticCollector diags;

    public AbsoluteAddressLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    /*
     * Hook triggered after the parser finishes evaluating a literal node.
     * We use this to catch raw memory addresses being used directly.
     */
    @Override
    public void exitLiteral(LinterParser.LiteralContext ctx)
    {
        // Skip if there's no mathematical expression or constant attached
        if (ctx.constExpr() == null)
            return;

        // If it's a raw hex number without any labels, it's likely a hardcoded memory address
        if (isHexOnlyConstExpr(ctx.constExpr()))
        {
            Token t = ctx.constExpr().getStart(); // Grab the token so we know exactly where the error is
            diags.report(Rules.AbsoluteAddress, Severity.WARNING, t, "Absolute address used; use a label/symbol instead of a magic address.");
        }
    }

    /*
     * Evaluates if a constant expression is purely a hexadecimal number.
     * It ensures no variables or labels (IDs) are mixed into the math.
     */
    private static boolean isHexOnlyConstExpr(LinterParser.ConstExprContext ctx)
    {
        // If it contains a label or symbol, it's safe (e.g., LABEL + 0x04)
        if (containsTokenType(ctx, LinterLexer.ID))
            return false;

        // Check if there is actually a hex value present
        return containsTokenType(ctx, LinterLexer.INT_HEX);
    }

    /*
     * Recursively searches through the parse tree to find a specific token type.
     * Very useful for digging into complex, nested math expressions.
     */
    private static boolean containsTokenType(ParseTree node, int tokenType)
    {
        if (node == null)
            return false;

        // Base case: we hit a leaf node (the actual token text)
        if (node instanceof TerminalNode)
        {
            TerminalNode t = (TerminalNode) node;

            if (t.getSymbol() == null)
                return false;

            if (t.getSymbol().getType() == tokenType)
                return true;

            return false;
        }

        // Recursive step: search through all children of this node
        int n = node.getChildCount();
        for (int i = 0; i < n; i++)
        {
            if (containsTokenType(node.getChild(i), tokenType))
                return true;
        }

        return false;
    }
}