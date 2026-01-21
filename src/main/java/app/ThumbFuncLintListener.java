package app;

import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ThumbFuncLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.THUMBFUNC;

    private final DiagnosticCollector diags;

    private final Map<String, Token> labelDefs;
    private final Set<String> calledLabels;
    private final Set<String> thumbFuncLabels;

    public ThumbFuncLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;

        this.labelDefs = new HashMap<>();
        this.calledLabels = new HashSet<>();
        this.thumbFuncLabels = new HashSet<>();
    }

    @Override
    public void exitLabelDef(LinterParser.LabelDefContext ctx)
    {
        Token t;
        String name;

        t = ctx.getStart();
        name = normalizeLabelDef(t.getText());

        if (name != null)
        {
            labelDefs.putIfAbsent(name, t);
        }
    }

    @Override
    public void exitRoutineWithThumbFunc(LinterParser.RoutineWithThumbFuncContext ctx)
    {
        Token labelToken;
        String name;

        labelToken = ctx.labelDef().getStart();
        name = normalizeLabelDef(labelToken.getText());

        if (name != null)
        {
            thumbFuncLabels.add(name);
            labelDefs.putIfAbsent(name, labelToken);
        }
    }

    @Override
    public void exitBranch(LinterParser.BranchContext ctx)
    {
        /*
            branch:
                BRANCH labelRef
              | BRANCHLINK labelRef
              | BRANCHLINKX (labelRef | register)
              | BRANCHX register
              | BRANXJAZELLE register
              ;
         */

        if (ctx.BRANCHLINK() != null && ctx.labelRef() != null)
        {
            calledLabels.add(ctx.labelRef().getText());
        }

        if (ctx.BRANCHLINKX() != null && ctx.labelRef() != null)
        {
            calledLabels.add(ctx.labelRef().getText());
        }
    }

    @Override
    public void exitProgram(LinterParser.ProgramContext ctx)
    {
        for (String name : calledLabels)
        {
            Token defToken;

            defToken = labelDefs.get(name);

            if (defToken == null)
            {
                continue;
            }

            if (thumbFuncLabels.contains(name))
            {
                continue;
            }

            diags.report(ruleId, Severity.ERROR, defToken, "Missing .thumb_func before routine label.");
        }
    }

    private static String normalizeLabelDef(String text)
    {
        if (text == null)
        {
            return null;
        }

        if (!text.endsWith(":"))
        {
            return null;
        }

        return text.substring(0, text.length() - 1);
    }
}
