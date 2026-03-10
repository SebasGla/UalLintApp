package app;

import antlr4gen.LinterLexer;
import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.List;

/*
 * Linting rule to ensure the .thumb_func directive is used before routine labels.
 * This is vital for ARM/Thumb interworking. Since the parser relies on this directive
 * to identify function blocks, missing it can cause routines to be "swallowed"
 * by the previous function's body.
 */
public final class ThumbFuncLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.ThumbFunc;

    private final DiagnosticCollector diags;
    private final List<Token> tokens; // The full list of tokens for looking backwards/forwards

    public ThumbFuncLintListener(DiagnosticCollector diags, CommonTokenStream tokenStream)
    {
        this.diags = diags;
        this.tokens = tokenStream.getTokens();
    }

    /*
     * Checks if a PUSH instruction that saves the Link Register (LR) is missing a .thumb_func.
     * If we see a "push {lr}" outside of a recognized routine, it's a strong sign
     * that a .thumb_func was forgotten above the nearest label.
     */
    @Override
    public void exitPush(LinterParser.PushContext ctx)
    {
        Token pushToken;
        int pushIndex;  // Position of the push in the token stream
        int labelIndex; // Position of the nearest label above the push

        // We only care about pushes that save the LR (standard routine prologue)
        if (!pushContainsLr(ctx))
        {
            return;
        }

        pushToken = ctx.getStart();
        pushIndex = pushToken.getTokenIndex();

        // Walk backwards to find the label this push belongs to
        labelIndex = findNearestLabelDefAbove(pushIndex);

        if (labelIndex < 0)
        {
            return;
        }

        // Error if that label doesn't have the required directive directly above it
        if (!hasThumbFuncDirectlyAboveLabel(labelIndex))
        {
            diags.report(ruleId, Severity.ERROR, pushToken, "Missing .thumb_func before routine label (detected via push {.., lr}).");
        }
    }

    /*
     * Checks if a 'bx lr' return is missing a .thumb_func.
     * Similar to the push check, if we find a return outside a routine,
     * it means the function boundaries were never established.
     */
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

        // Only care about 'bx lr' (the standard return instruction)
        if (!"lr".equalsIgnoreCase(ctx.register().getText()))
        {
            return;
        }

        bxToken = ctx.getStart();
        bxIndex = bxToken.getTokenIndex();

        labelIndex = findNearestLabelDefAbove(bxIndex);

        // If we found a label and it HAS a .thumb_func, then it's a valid return
        if (labelIndex >= 0 && hasThumbFuncDirectlyAboveLabel(labelIndex))
        {
            return;
        }

        diags.report(ruleId, Severity.ERROR, bxToken, "Missing .thumb_func before routine label (detected via bx lr).");
    }

    /*
     * Heuristic check for "swallowed" routines.
     * Sometimes a label appears INSIDE a routine. If that label is followed
     * by a push and a return, it's almost certainly a new routine that
     * the parser accidentally included in the previous one because of a missing directive.
     */
    @Override
    public void exitLabelDef(LinterParser.LabelDefContext ctx)
    {
        Token labelToken;
        int labelIndex;

        // This check only applies to labels that the parser thinks are "inner" labels
        if (!isInsideRoutine(ctx))
        {
            return;
        }

        labelToken = ctx.getStart();
        labelIndex = labelToken.getTokenIndex();

        if (hasThumbFuncDirectlyAboveLabel(labelIndex))
        {
            return;
        }

        // If it looks like a function (push... return) but is missing the directive, warn the user
        if (looksLikeSwallowedRoutine(labelIndex))
        {
            diags.report(ruleId, Severity.WARNING, labelToken, " Possible missing .thumb_func before routine label (manual check needed).");
        }
    }

    /*
     * Scans the tokens following a label to see if it acts like a function.
     * It checks if the block ends in a return and follows an existing dead-code path,
     * or if it has an explicit PUSH before returning.
     */
    private boolean looksLikeSwallowedRoutine(int labelIndex)
    {
        int returnIndex = findNextReturnAfter(labelIndex, 50);

        if (returnIndex >= 0 && !existsLabelBetween(labelIndex, returnIndex))
        {
            // If the preceding instruction was a return/unconditional jump,
            // control flow is dead. This label is highly likely a new routine entry point.
            if (isFollowingDeadCode(labelIndex))
            {
                return true;
            }

            // Alternatively, if there is a PUSH before the return, it's a routine
            int nextPushIndex = findNextTokenOfTypeAfter(labelIndex, LinterLexer.PUSH, 50);
            if (nextPushIndex >= 0 && nextPushIndex < returnIndex)
            {
                return true;
            }
        }

        return false;
    }

    /*
     * Helper to find the next return instruction (bx lr OR pop pc) in the token stream.
     */
    private int findNextReturnAfter(int fromIndexExclusive, int maxLookaheadTokens)
    {
        int end = Math.min(tokens.size(), fromIndexExclusive + Math.max(1, maxLookaheadTokens));

        for (int i = fromIndexExclusive + 1; i < end; i++)
        {
            if (tokens.get(i).getType() == LinterLexer.BRANCHX && isBxLrAtIndex(i))
            {
                return i;
            }
            if (tokens.get(i).getType() == LinterLexer.POP && isPopPcAtIndex(i))
            {
                return i;
            }
        }

        return -1;
    }

    /*
     * Verifies if a POP instruction includes the Program Counter (PC).
     */
    private boolean isPopPcAtIndex(int popIndex)
    {
        int end = Math.min(tokens.size(), popIndex + 20);

        for (int i = popIndex + 1; i < end; i++)
        {
            Token tok = tokens.get(i);

            // If we hit a new label before finding 'pc', stop
            if (tok.getType() == LinterLexer.LABEL_DEF)
            {
                return false;
            }

            if ("pc".equalsIgnoreCase(tok.getText()))
            {
                return true;
            }
        }

        return false;
    }

    /*
     * Checks if the token immediately preceding the label is a dead-code instruction
     * (meaning the label is an isolated entry point).
     */
    private boolean isFollowingDeadCode(int labelIndex)
    {
        for (int i = labelIndex - 1; i >= 0; i--)
        {
            Token t = tokens.get(i);

            // Skip comments and whitespace
            if (t.getChannel() != Token.DEFAULT_CHANNEL)
            {
                continue;
            }

            int type = t.getType();

            // Skip directives and previous labels
            if (type == LinterLexer.LABEL_DEF || type == LinterLexer.THUMBFUNC || type == LinterLexer.CONSTANT)
            {
                continue;
            }

            // If the last real instruction was a return or unconditional branch, we are following dead code
            if (type == LinterLexer.BRANCHX && isBxLrAtIndex(i)) return true;
            if (type == LinterLexer.POP && isPopPcAtIndex(i)) return true;
            if (type == LinterLexer.BRANCH)
            {
                String text = t.getText().toLowerCase();
                if (text.equals("b") || text.equals("bal")) return true;
            }

            // We hit a normal instruction, so control flow naturally falls into this label
            return false;
        }

        return true; // Start of file
    }

    /*
     * Verifies if a specific branch instruction is actually targeting the Link Register.
     */
    private boolean isBxLrAtIndex(int branchxIndex)
    {
        int i;
        int end;
        Token tok;

        end = Math.min(tokens.size(), branchxIndex + 20);

        for (i = branchxIndex + 1; i < end; i++)
        {
            tok = tokens.get(i);

            // If we hit a new label before finding 'lr', this wasn't a return for this block
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

    /*
     * Checks if any labels exist in a specific range of tokens.
     */
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

    /*
     * General lookahead helper to find a specific token type.
     */
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

    /*
     * Utility to check if a node is part of a routine branch in the parse tree.
     */
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

    /*
     * Walks backward through the token stream to find the closest label definition.
     */
    private int findNearestLabelDefAbove(int fromIndexExclusive)
    {
        int i;
        Token t;

        for (i = fromIndexExclusive - 1; i >= 0; i--)
        {
            t = tokens.get(i);

            // Only check the main code channel (ignore comments/whitespace)
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

    /*
     * Verifies if the .thumb_func directive is present exactly one slot above a label.
     */
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

    /*
     * Skips hidden tokens (whitespace/comments) to find the actual previous instruction.
     */
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

    /*
     * Checks if the PUSH instruction includes the Link Register.
     */
    private boolean pushContainsLr(LinterParser.PushContext ctx)
    {
        if (ctx == null || ctx.regList() == null)
        {
            return false;
        }

        return regListContainsLr(ctx.regList());
    }

    /*
     * Scans a register list block for the 'lr' keyword.
     */
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

            // Check the upper bound of a range (e.g. {r0-lr})
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