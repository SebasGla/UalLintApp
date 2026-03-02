package app;

import antlr4gen.LinterLexer;
import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

public final class ThumbFuncLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.ThumbFunc;

    private final DiagnosticCollector diags;
    private final List<Token> tokens;

    public ThumbFuncLintListener(DiagnosticCollector diags, CommonTokenStream tokenStream)
    {
        this.diags = diags;
        this.tokens = tokenStream.getTokens();
    }

    @Override
    public void exitPush(LinterParser.PushContext ctx)
    {
        Token pushToken;
        int pushIndex;
        int labelIndex;

        if (isInsideRoutine(ctx))
        {
            return;
        }

        if (!pushContainsLr(ctx))
        {
            return;
        }

        pushToken = ctx.getStart();
        pushIndex = pushToken.getTokenIndex();

        labelIndex = findNearestLabelDefAbove(pushIndex);

        if (labelIndex < 0)
        {
            return;
        }

        if (!hasThumbFuncDirectlyAboveLabel(labelIndex))
        {
            diags.report(ruleId, Severity.ERROR, pushToken, "Missing .thumb_func before routine label (detected via push {.., lr}).");
        }
    }

    @Override
    public void exitBranch(LinterParser.BranchContext ctx)
    {
        Token bxToken;
        int bxIndex;
        int labelIndex;

        if (isInsideRoutine(ctx))
        {
            return;
        }

        if (ctx.BRANCHX() == null)
        {
            return;
        }

        if (ctx.register() == null)
        {
            return;
        }

        if (!"lr".equalsIgnoreCase(ctx.register().getText()))
        {
            return;
        }

        bxToken = ctx.getStart();
        bxIndex = bxToken.getTokenIndex();

        /*
            Guard: do not flag if bx lr is associated with a label that has a proper .thumb_func above it.
            This avoids false positives when bx lr is parsed as normal branch.
         */
        labelIndex = findNearestLabelDefAbove(bxIndex);

        if (labelIndex >= 0 && hasThumbFuncDirectlyAboveLabel(labelIndex))
        {
            return;
        }

        diags.report(ruleId, Severity.ERROR, bxToken, "Missing .thumb_func before routine label (detected via bx lr).");
    }

    @Override
    public void exitLabelDef(LinterParser.LabelDefContext ctx)
    {
        Token labelToken;
        int labelIndex;

        /*
            Only run this heuristic for labels that occur INSIDE a parsed .thumb_func routine body.
            These are candidates for being "swallowed" routine starts (like bad_routine4:).
         */
        if (!isInsideRoutine(ctx))
        {
            return;
        }

        labelToken = ctx.getStart();
        labelIndex = labelToken.getTokenIndex();

        /*
            If this inner label already has a .thumb_func above it, it is fine.
         */
        if (hasThumbFuncDirectlyAboveLabel(labelIndex))
        {
            return;
        }

        /*
            Heuristic: label inside a routine that is followed by a push and later a bx lr
            is likely the start of a second routine that got swallowed by the grammar.
         */
        if (looksLikeSwallowedRoutine(labelIndex))
        {
            diags.report(ruleId, Severity.WARNING, labelToken, " Possible missing .thumb_func before routine label (manual check needed).");
        }
    }

    private boolean looksLikeSwallowedRoutine(int labelIndex)
    {
        int nextPushIndex;
        int bxIndex;

        nextPushIndex = findNextTokenOfTypeAfter(labelIndex, LinterLexer.PUSH, 50);

        if (nextPushIndex < 0)
        {
            return false;
        }

        /*
            Require that the push occurs before the next label (otherwise it might belong to another block).
         */
        if (existsLabelBetween(labelIndex, nextPushIndex))
        {
            return false;
        }

        /*
            Require a bx lr after the push, before the next label. This catches your bad_routine4 form.
         */
        bxIndex = findNextBxLrAfter(nextPushIndex, 300);

        if (bxIndex < 0)
        {
            return false;
        }

        if (existsLabelBetween(labelIndex, bxIndex))
        {
            return false;
        }

        return true;
    }

    private int findNextBxLrAfter(int fromIndexExclusive, int maxLookaheadTokens)
    {
        int i;
        int end;

        end = Math.min(tokens.size(), fromIndexExclusive + Math.max(1, maxLookaheadTokens));

        for (i = fromIndexExclusive + 1; i < end; i++)
        {
            if (tokens.get(i).getType() == LinterLexer.BRANCHX)
            {
                if (isBxLrAtIndex(i))
                {
                    return i;
                }
            }
        }

        return -1;
    }

    private boolean isBxLrAtIndex(int branchxIndex)
    {
        int i;
        int end;
        Token tok;

        end = Math.min(tokens.size(), branchxIndex + 20);

        for (i = branchxIndex + 1; i < end; i++)
        {
            tok = tokens.get(i);

            if (tok.getType() == LinterLexer.LABEL_DEF)
            {
                return false;
            }

            if ("lr".equalsIgnoreCase(tok.getText()))
            {
                return true;
            }
        }

        return false;
    }

    private boolean existsLabelBetween(int fromIndexExclusive, int toIndexExclusive)
    {
        int i;

        for (i = fromIndexExclusive + 1; i < toIndexExclusive && i < tokens.size(); i++)
        {
            if (tokens.get(i).getType() == LinterLexer.LABEL_DEF)
            {
                return true;
            }
        }

        return false;
    }

    private int findNextTokenOfTypeAfter(int fromIndexExclusive, int tokenType, int maxLookaheadTokens)
    {
        int i;
        int end;

        end = Math.min(tokens.size(), fromIndexExclusive + Math.max(1, maxLookaheadTokens));

        for (i = fromIndexExclusive + 1; i < end; i++)
        {
            if (tokens.get(i).getType() == tokenType)
            {
                return i;
            }
        }

        return -1;
    }

    private boolean isInsideRoutine(ParseTree node)
    {
        ParseTree p;

        p = node;

        while (p != null)
        {
            if (p instanceof LinterParser.RoutineContext)
            {
                return true;
            }

            p = p.getParent();
        }

        return false;
    }

    private int findNearestLabelDefAbove(int fromIndexExclusive)
    {
        int i;
        Token t;

        for (i = fromIndexExclusive - 1; i >= 0; i--)
        {
            t = tokens.get(i);

            if (t.getChannel() != Token.DEFAULT_CHANNEL)
            {
                continue;
            }

            if (t.getType() == LinterLexer.LABEL_DEF)
            {
                return i;
            }
        }

        return -1;
    }

    private boolean hasThumbFuncDirectlyAboveLabel(int labelIndex)
    {
        int prevIndex;

        prevIndex = findPreviousDefaultChannelTokenIndex(labelIndex);

        if (prevIndex < 0)
        {
            return false;
        }

        return tokens.get(prevIndex).getType() == LinterLexer.THUMBFUNC;
    }

    private int findPreviousDefaultChannelTokenIndex(int fromIndexExclusive)
    {
        int i;

        for (i = fromIndexExclusive - 1; i >= 0; i--)
        {
            if (tokens.get(i).getChannel() == Token.DEFAULT_CHANNEL)
            {
                return i;
            }
        }

        return -1;
    }

    private boolean pushContainsLr(LinterParser.PushContext ctx)
    {
        if (ctx == null || ctx.regList() == null)
        {
            return false;
        }

        return regListContainsLr(ctx.regList());
    }

    private boolean regListContainsLr(LinterParser.RegListContext regList)
    {
        int i;
        LinterParser.RegElemContext elem;
        String t1;
        String t2;

        for (i = 0; i < regList.regElem().size(); i++)
        {
            elem = regList.regElem(i);

            t1 = elem.register(0).getText();

            if ("lr".equalsIgnoreCase(t1))
            {
                return true;
            }

            if (elem.register().size() > 1)
            {
                t2 = elem.register(1).getText();

                if ("lr".equalsIgnoreCase(t2))
                {
                    return true;
                }
            }
        }

        return false;
    }

}
