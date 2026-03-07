package app;

import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.List;

/*
 * Linting rule to detect and warn about recursive function calls.
 * In embedded systems and strict coding standards (like MISRA), recursion
 * is heavily discouraged because it makes stack usage unpredictable and
 * can easily lead to stack overflows on hardware with limited memory.
 */
public final class RecursiveRoutineLintListener extends LinterParserBaseListener
{
    // NOTE: Make sure 'RecursiveRoutine' is added to your Rules enum!
    private static final Rules ruleId = Rules.RecursiveRoutine;

    private final DiagnosticCollector diags;

    public RecursiveRoutineLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    /*
     * Triggered when the parser enters a new function block.
     * We grab the function's name and scan its entire body to see if it ever
     * tries to branch (call) back to its own name.
     */
    @Override
    public void enterRoutine(LinterParser.RoutineContext ctx)
    {
        LinterParser.LabelDefContext labelCtx;
        String rawLabel;
        String routineName;
        LinterParser.RoutineBodyContext body;
        List<LinterParser.BranchContext> branches;

        labelCtx = ctx.labelDef();

        // Safety check: ensure we actually have a label
        if (labelCtx == null || labelCtx.LABEL_DEF() == null)
        {
            return;
        }

        rawLabel = labelCtx.LABEL_DEF().getText();

        // Strip the trailing colon from the label (e.g., "my_func:" becomes "my_func")
        routineName = rawLabel.substring(0, rawLabel.length() - 1);

        body = ctx.routineBody();

        if (body == null)
        {
            return;
        }

        branches = new ArrayList<>();

        // Find every single branch instruction inside this function
        collectBranches(body, branches);

        // Check if any of those branches point back to this exact function
        for (LinterParser.BranchContext b : branches)
        {
            LinterParser.LabelRefContext labelRef;
            String targetName;

            labelRef = b.labelRef();

            // Ignore register branches (like bx lr) since we can't statically resolve their targets
            if (labelRef == null)
            {
                continue;
            }

            targetName = labelRef.getText();

            // If the target matches the function name, we caught a recursive call!
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

    /*
     * Helper method to recursively dig through the AST and extract every BranchContext
     * we can find inside a specific node (like the routine body).
     */
    private void collectBranches(ParseTree node, List<LinterParser.BranchContext> out)
    {
        if (node == null)
        {
            return;
        }

        // Base case: we found a branch, add it to our list
        if (node instanceof LinterParser.BranchContext)
        {
            out.add((LinterParser.BranchContext) node);
            return; // Branches don't contain other branches, so we can stop digging here
        }

        int n;

        n = node.getChildCount();

        // Recursive step: keep digging into child nodes
        for (int i = 0; i < n; i++)
        {
            collectBranches(
                    node.getChild(i),
                    out
            );
        }
    }
}