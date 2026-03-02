package app;

import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.Token;

import java.util.ArrayList;
import java.util.List;

public final class NoCondReturnsLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.NoCondReturns;

    private final DiagnosticCollector diags;

    private RoutineState current;

    public NoCondReturnsLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
        this.current = null;
    }

    @Override
    public void enterRoutine(LinterParser.RoutineContext ctx)
    {
        Token labelToken;

        labelToken = ctx.labelDef().getStart();

        current = new RoutineState(labelToken);
    }

    @Override
    public void exitRoutine(LinterParser.RoutineContext ctx)
    {
        if (current == null)
        {
            return;
        }

        if (current.conditionalReturnTokens.size() > 0)
        {
            diags.report(ruleId, Severity.ERROR, current.conditionalReturnTokens.get(0),
                    "Conditional return detected. Functions must have a single unconditional return path.");
        }

        if (current.returnTokens.size() > 1)
        {
            diags.report(ruleId, Severity.ERROR, current.returnTokens.get(1),
                    "Multiple return sites detected. Functions must have a single unconditional return path.");
        }

        current = null;
    }

    @Override
    public void exitRoutineBX(LinterParser.RoutineBXContext ctx)
    {
        if (current == null)
        {
            return;
        }

        recordReturn(ctx.getStart(), false);
    }

    @Override
    public void exitBranch(LinterParser.BranchContext ctx)
    {
        Token bxToken;
        String opText;

        if (current == null)
        {
            return;
        }

        if (ctx.BRANCHX() == null || ctx.register() == null)
        {
            return;
        }

        if (!"lr".equalsIgnoreCase(ctx.register().getText()))
        {
            return;
        }

        bxToken = ctx.getStart();
        opText = bxToken.getText();

        /*
            Heuristic:
            - "bx" => unconditional
            - "bxeq", "bxne", ... => conditional
         */
        if (opText != null && opText.length() > 2)
        {
            recordReturn(bxToken, true);
            return;
        }

        recordReturn(bxToken, false);
    }

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
