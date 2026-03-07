package app;

import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

/*
 * Linting rule to enforce a single, unconditional return point for every function.
 * Having multiple exit points or conditional returns (like bxne lr) makes
 * control flow much harder to read and violates strict coding standards (like MISRA).
 */
public final class NoCondReturnsLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.NoCondReturns;

    private final DiagnosticCollector diags;

    // Tracks the state of the function we are currently inside
    private RoutineState current;

    public NoCondReturnsLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
        this.current = null;
    }

    /*
     * Hook triggered when the parser enters a new routine (function).
     * We initialize a new tracker to start counting how many times this function tries to return.
     */
    @Override
    public void enterRoutine(LinterParser.RoutineContext ctx)
    {
        Token labelToken;

        labelToken = ctx.labelDef().getStart();

        current = new RoutineState(labelToken);
    }

    /*
     * Hook triggered when the parser leaves the routine.
     * This is where we evaluate everything we found inside the function and fire warnings.
     */
    @Override
    public void exitRoutine(LinterParser.RoutineContext ctx)
    {
        if (current == null)
        {
            return;
        }

        // Rule 1: No conditional returns allowed (e.g., bxeq lr)
        if (current.conditionalReturnTokens.size() > 0)
        {
            diags.report(ruleId, Severity.ERROR, current.conditionalReturnTokens.get(0),
                    "Conditional return detected. Functions must have a single unconditional return path.");
        }

        // Rule 2: Only one return statement per function
        if (current.returnTokens.size() > 1)
        {
            diags.report(ruleId, Severity.ERROR, current.returnTokens.get(1),
                    "Multiple return sites detected. Functions must have a single unconditional return path.");
        }

        // Clean up state for the next routine
        current = null;
    }

    /*
     * Catches the standard 'bx lr' that the grammar automatically identifies at the end of a routine block.
     */
    @Override
    public void exitRoutineBX(LinterParser.RoutineBXContext ctx)
    {
        if (current == null)
        {
            return;
        }

        recordReturn(ctx.getStart(), false);
    }

    /*
     * Catches manual branch instructions jumping back to the caller using the Link Register (lr).
     */
    @Override
    public void exitBranch(LinterParser.BranchContext ctx)
    {
        Token bxToken;
        String opText;

        if (current == null)
        {
            return;
        }

        // We only care about BX instructions targeting a specific register
        if (ctx.BRANCHX() == null || ctx.register() == null)
        {
            return;
        }

        // Returning from a function is specifically done by branching to 'lr'
        if (!"lr".equalsIgnoreCase(ctx.register().getText()))
        {
            return;
        }

        bxToken = ctx.getStart();
        opText = bxToken.getText();

        /*
         * Heuristic:
         * - "bx" is exactly 2 characters => unconditional
         * - "bxeq", "bxne" are longer => conditional
         */
        if (opText != null && opText.length() > 2)
        {
            recordReturn(bxToken, true);
            return;
        }

        recordReturn(bxToken, false);
    }

    /*
     * Catches returns executed via POP instructions (e.g., pop {r0, r1, pc}).
     * Popping directly into the Program Counter (pc) instantly jumps execution.
     */
    @Override
    public void exitPop(LinterParser.PopContext ctx)
    {
        if (current == null)
        {
            return;
        }

        if (regListContainsPc(ctx.regList()))
        {
            recordReturn(ctx.getStart(), false);
        }
    }

    private void recordReturn(Token tok, boolean isConditional)
    {
        current.returnTokens.add(tok);

        if (isConditional)
        {
            current.conditionalReturnTokens.add(tok);
        }
    }

    /*
     * Scans a register list (like inside a PUSH or POP) to see if the Program Counter (PC) is included.
     * Handles both single registers and register ranges (like r0-r3).
     */
    private boolean regListContainsPc(LinterParser.RegListContext regList)
    {
        int i;
        LinterParser.RegElemContext elem;
        String t1;
        String t2;

        if (regList == null)
        {
            return false;
        }

        for (i = 0; i < regList.regElem().size(); i++)
        {
            elem = regList.regElem(i);

            t1 = elem.register(0).getText();

            if ("pc".equalsIgnoreCase(t1))
            {
                return true;
            }

            // Check the upper bound if it's a range (e.g., r0-pc)
            if (elem.register().size() > 1)
            {
                t2 = elem.register(1).getText();

                if ("pc".equalsIgnoreCase(t2))
                {
                    return true;
                }
            }
        }

        return false;
    }

    /*
     * Simple data container to track the return statements found inside the current routine.
     */
    private static final class RoutineState
    {
        private final Token labelToken;
        private final List<Token> returnTokens;
        private final List<Token> conditionalReturnTokens;

        private RoutineState(Token labelToken)
        {
            this.labelToken = labelToken;
            this.returnTokens = new ArrayList<>();
            this.conditionalReturnTokens = new ArrayList<>();
        }
    }

}