package app;

import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

public final class RecursiveRoutineLintListener extends LinterParserBaseListener
{
    // HINWEIS: Du musst RECURSIVEROUTINE zu deinem Rules-Enum hinzufügen!
    private static final Rules ruleId = Rules.RecursiveRoutine;

    private final DiagnosticCollector diags;

    public RecursiveRoutineLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    @Override
    public void enterRoutine(LinterParser.RoutineContext ctx)
    {
        LinterParser.LabelDefContext labelCtx;
        String rawLabel;
        String routineName;
        LinterParser.RoutineBodyContext body;
        List<LinterParser.BranchContext> branches;

        labelCtx = ctx.labelDef();

        if (labelCtx == null || labelCtx.LABEL_DEF() == null)
        {
            return;
        }

        rawLabel = labelCtx.LABEL_DEF().getText();

        // Entferne den Doppelpunkt am Ende des Labels (z.B. "my_func:" -> "my_func")
        routineName = rawLabel.substring(0, rawLabel.length() - 1);

        body = ctx.routineBody();

        if (body == null)
        {
            return;
        }

        branches = new ArrayList<>();

        collectBranches(body, branches);

        for (LinterParser.BranchContext b : branches)
        {
            LinterParser.LabelRefContext labelRef;
            String targetName;

            labelRef = b.labelRef();

            // Bei Sprüngen über Register (z.B. bx r0) ist labelRef null
            if (labelRef == null)
            {
                continue;
            }

            targetName = labelRef.getText();

            if (routineName.equals(targetName))
            {
                diags.report(
                        ruleId,
                        Severity.WARNING,
                        b.getStart(),
                        "Recursive call detected: Routine '" + routineName + "' calls itself."
                );
            }
        }
    }

    private void collectBranches(ParseTree node, List<LinterParser.BranchContext> out)
    {
        if (node == null)
        {
            return;
        }

        if (node instanceof LinterParser.BranchContext)
        {
            out.add((LinterParser.BranchContext) node);
            return;
        }

        int n;

        n = node.getChildCount();

        for (int i = 0; i < n; i++)
        {
            collectBranches(
                    node.getChild(i),
                    out
            );
        }
    }
}