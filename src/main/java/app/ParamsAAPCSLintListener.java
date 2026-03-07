package app;

import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.HashSet;
import java.util.Set;

/*
 * Linting rule to enforce the ARM Architecture Procedure Call Standard (AAPCS).
 * It ensures two main things:
 * 1. If you use r4-r11 (callee-saved), you must save them on the stack first.
 * 2. Parameters (r0-r3) must be used in order (e.g., don't use r2 if you haven't used r0 and r1).
 */
public final class ParamsAAPCSLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.ParamsAAPCS;

    private final DiagnosticCollector diags;

    // State tracker for the function currently being parsed
    private RoutineState current;

    public ParamsAAPCSLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
        this.current = null;
    }

    /*
     * Resets the tracker when we enter a new routine.
     */
    @Override
    public void enterRoutine(LinterParser.RoutineContext ctx)
    {
        current = new RoutineState(ctx.labelDef().getStart());
    }

    /*
     * Evaluates everything we tracked inside the routine and fires warnings if
     * the AAPCS rules were broken.
     */
    @Override
    public void exitRoutine(LinterParser.RoutineContext ctx)
    {
        int r;

        if (current == null)
        {
            return;
        }

        // Check if any used callee-saved register (r4..r11) was forgotten in the prologue push.
        for (r = 4; r <= 11; r++)
        {
            if (current.usedCalleeSaved.contains(r) && !current.savedInPrologue.contains(r))
            {
                diags.report(ruleId, Severity.ERROR, current.routineLabel,
                        "AAPCS: callee-saved register r" + r + " is used but not pushed on the stack.");
            }
        }

        // Check if r0-r3 were used out of order (e.g., using r2 without using r0 and r1)
        if (violatesParamPrefixRule())
        {
            diags.report(
                    ruleId,
                    Severity.ERROR,
                    current.routineLabel,
                    "AAPCS (strict): parameter registers r0–r3 must be used in ascending order.\n"
                            + "  Inferred parameters: " + formatParamSet(current.argIsParam) + "\n"
                            + "  Expected parameters: " + formatExpectedPrefix(current.argIsParam) + "\n"
                            + "  Missing: " + formatMissingFromPrefix(current.argIsParam)
            );
        }

        current = null;
    }

    /*
     * Hooks into PUSH instructions to record which registers are saved at the
     * very beginning of the function (the prologue).
     */
    @Override
    public void exitPush(LinterParser.PushContext ctx)
    {
        Token pushToken;

        if (current == null)
        {
            return;
        }

        // Only care about PUSH instructions that are directly in the main routine body
        if (!(ctx.getParent() instanceof LinterParser.RoutineBodyContext))
        {
            return;
        }

        pushToken = ctx.getStart();

        addRegListToSet(ctx.regList(), current.savedInPrologue);

        // r0-r3 are for arguments, they shouldn't be blindly pushed in the prologue
        if (current.savedInPrologue.contains(0)
                || current.savedInPrologue.contains(1)
                || current.savedInPrologue.contains(2)
                || current.savedInPrologue.contains(3))
        {
            diags.report(ruleId, Severity.ERROR, pushToken,
                    "AAPCS: push contains r0–r3.");
        }
    }

    /*
     * Intercepts every register used anywhere in the function.
     * Tracks if it was read or written to figure out if it's an incoming parameter.
     */
    @Override
    public void exitRegister(LinterParser.RegisterContext ctx)
    {
        int reg;

        if (current == null)
        {
            return;
        }

        // Ignore registers that are just sitting in a {r0, r1} list block
        if (isInsideRegList(ctx))
        {
            return;
        }

        reg = parseRegisterNumber(ctx.getText());

        // Track callee-saved usage
        if (reg >= 4 && reg <= 11)
        {
            current.usedCalleeSaved.add(reg);
        }

        // Track parameter usage (r0-r3)
        if (reg >= 0 && reg <= 3)
        {
            if (isDestinationRegisterOccurrence(ctx))
            {
                // If it's the destination, but also read from (like `ADD R0, R1`), it's a parameter
                if (destinationAlsoRead(ctx))
                {
                    if (!current.argWritten[reg])
                    {
                        current.argIsParam[reg] = true;
                    }
                }

                current.argWritten[reg] = true;
                return;
            }

            // If a register is read before it's ever written, it MUST be an incoming parameter
            if (!current.argWritten[reg])
            {
                current.argIsParam[reg] = true;
            }
        }
    }

    /*
     * Checks if there are gaps in the used parameter registers.
     * Returns true if r2 is used, but r1 or r0 was skipped.
     */
    private boolean violatesParamPrefixRule()
    {
        int highest;
        int i;

        if (current == null)
        {
            return false;
        }

        highest = -1;

        for (i = 0; i < 4; i++)
        {
            if (current.argIsParam[i])
            {
                highest = i;
            }
        }

        for (i = 0; i < highest; i++)
        {
            if (!current.argIsParam[i])
            {
                return true; // Found a gap!
            }
        }

        return false;
    }

    /*
     * Climbs up the AST to figure out if this specific register occurrence is
     * on the receiving end (destination) of an instruction.
     */
    private boolean isDestinationRegisterOccurrence(LinterParser.RegisterContext regCtx)
    {
        ParseTree p;

        p = regCtx.getParent();

        while (p != null)
        {
            if (p instanceof LinterParser.MovInstrContext)
            {
                return regCtx == ((LinterParser.MovInstrContext) p).register();
            }

            if (p instanceof LinterParser.MovwInstrContext)
            {
                return regCtx == ((LinterParser.MovwInstrContext) p).register();
            }

            if (p instanceof LinterParser.MovtInstrContext)
            {
                return regCtx == ((LinterParser.MovtInstrContext) p).register();
            }

            if (p instanceof LinterParser.Mov32InstrContext)
            {
                return regCtx == ((LinterParser.Mov32InstrContext) p).register();
            }

            if (p instanceof LinterParser.AdrInstrContext)
            {
                return regCtx == ((LinterParser.AdrInstrContext) p).register();
            }

            if (p instanceof LinterParser.LdrInstrContext)
            {
                return regCtx == ((LinterParser.LdrInstrContext) p).register();
            }

            if (p instanceof LinterParser.ArithmLongContext)
            {
                return regCtx == ((LinterParser.ArithmLongContext) p).rd;
            }

            if (p instanceof LinterParser.ArithmShortContext)
            {
                return regCtx == ((LinterParser.ArithmShortContext) p).rd;
            }

            if (p instanceof LinterParser.LogicLongContext)
            {
                return regCtx == ((LinterParser.LogicLongContext) p).rd;
            }

            if (p instanceof LinterParser.LogicShortContext)
            {
                return regCtx == ((LinterParser.LogicShortContext) p).rd;
            }

            if (p instanceof LinterParser.MulLongContext)
            {
                return regCtx == ((LinterParser.MulLongContext) p).rd;
            }

            if (p instanceof LinterParser.MulShortContext)
            {
                return regCtx == ((LinterParser.MulShortContext) p).rd;
            }

            if (p instanceof LinterParser.DivInstrContext)
            {
                LinterParser.DivInstrContext d;

                d = (LinterParser.DivInstrContext) p;

                if (d.register() != null && d.register().size() > 0)
                {
                    return regCtx == d.register(0);
                }

                return false;
            }

            if (p instanceof LinterParser.ShiftLongContext)
            {
                return regCtx == ((LinterParser.ShiftLongContext) p).rd;
            }

            if (p instanceof LinterParser.ShiftShortContext)
            {
                return regCtx == ((LinterParser.ShiftShortContext) p).rd;
            }

            if (p instanceof LinterParser.RrxIntrstuctionContext)
            {
                LinterParser.RrxIntrstuctionContext r;

                r = (LinterParser.RrxIntrstuctionContext) p;

                if (r.register() != null && r.register().size() > 0)
                {
                    return regCtx == r.register(0);
                }

                return false;
            }

            if (p instanceof LinterParser.MulASInstrContext)
            {
                LinterParser.MulASInstrContext m;

                m = (LinterParser.MulASInstrContext) p;

                if (m.register() != null && m.register().size() > 0)
                {
                    return regCtx == m.register(0);
                }

                return false;
            }

            // CMP and TST only set flags, they don't write data to the register
            // So we intentionally do NOT treat them as writes here.

            p = p.getParent();
        }

        return false;
    }

    /*
     * Checks if the register is inside a block like {r0, r1, r2}
     */
    private boolean isInsideRegList(ParseTree node)
    {
        ParseTree p;

        p = node;

        while (p != null)
        {
            if (p instanceof LinterParser.RegListContext)
            {
                return true;
            }

            p = p.getParent();
        }

        return false;
    }

    /*
     * Parses a register list (e.g. from a PUSH command) and adds the extracted
     * register numbers to a tracking set. Handles ranges like r0-r3 automatically.
     */
    private void addRegListToSet(LinterParser.RegListContext regList, Set<Integer> out)
    {
        int i;
        LinterParser.RegElemContext elem;
        int lo;
        int hi;

        if (regList == null)
        {
            return;
        }

        for (i = 0; i < regList.regElem().size(); i++)
        {
            elem = regList.regElem(i);

            lo = parseRegisterNumber(elem.register(0).getText());

            if (lo >= 0)
            {
                out.add(lo);
            }

            // If there's a second register, it's a range (e.g., r4-r7)
            if (elem.register().size() > 1)
            {
                hi = parseRegisterNumber(elem.register(1).getText());

                if (hi >= 0)
                {
                    out.add(hi);
                }

                if (lo >= 0 && hi >= 0)
                {
                    addRegisterRange(out, lo, hi);
                }
            }
        }
    }

    private void addRegisterRange(Set<Integer> out, int a, int b)
    {
        int min;
        int max;
        int r;

        min = Math.min(a, b);
        max = Math.max(a, b);

        for (r = min; r <= max; r++)
        {
            out.add(r);
        }
    }

    /*
     * Helper to turn the string "r4" into the integer 4.
     */
    private int parseRegisterNumber(String text)
    {
        String s;

        if (text == null)
        {
            return -1;
        }

        s = text.trim();

        if (s.length() < 2)
        {
            return -1;
        }

        if (s.charAt(0) != 'r' && s.charAt(0) != 'R')
        {
            return -1;
        }

        try
        {
            return Integer.parseInt(s.substring(1));
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    /*
     * Holds the tracking state for the current function being analyzed.
     */
    private static final class RoutineState
    {
        private final Token routineLabel;
        private final Set<Integer> savedInPrologue;
        private final Set<Integer> usedCalleeSaved;

        // Tracks if r0-r3 have been written to or read from to infer if they are parameters
        private final boolean[] argWritten;
        private final boolean[] argIsParam;

        private RoutineState(Token routineLabel)
        {
            this.routineLabel = routineLabel;
            this.savedInPrologue = new HashSet<>();
            this.usedCalleeSaved = new HashSet<>();
            this.argWritten = new boolean[4];
            this.argIsParam = new boolean[4];
        }
    }

    /*
     * Edge case handler: In short-form instructions like "ADD R0, R1", R0 is both
     * the destination AND a source. We need to know this to properly infer arguments.
     */
    private boolean destinationAlsoRead(LinterParser.RegisterContext regCtx)
    {
        ParseTree p;
        String rdText;

        p = regCtx.getParent();

        while (p != null)
        {
            if (p instanceof LinterParser.LogicLongContext)
            {
                LinterParser.LogicLongContext l;

                l = (LinterParser.LogicLongContext) p;

                if (regCtx != l.rd)
                {
                    return false;
                }

                rdText = l.rd.getText();

                return rdText != null && rdText.equalsIgnoreCase(l.rn.getText());
            }

            if (p instanceof LinterParser.LogicShortContext)
            {
                LinterParser.LogicShortContext l;
                LinterParser.Op2Context op2;

                l = (LinterParser.LogicShortContext) p;

                if (regCtx != l.rd)
                {
                    return false;
                }

                rdText = l.rd.getText();
                op2 = l.rn;

                if (op2 == null || op2.register() == null)
                {
                    return false;
                }

                return rdText != null && rdText.equalsIgnoreCase(op2.register().getText());
            }

            if (p instanceof LinterParser.ArithmLongContext)
            {
                LinterParser.ArithmLongContext a;

                a = (LinterParser.ArithmLongContext) p;

                if (regCtx != a.rd)
                {
                    return false;
                }

                rdText = a.rd.getText();

                return rdText != null && rdText.equalsIgnoreCase(a.rn.getText());
            }

            if (p instanceof LinterParser.ArithmShortContext)
            {
                LinterParser.ArithmShortContext a;
                LinterParser.Op2Context op2;

                a = (LinterParser.ArithmShortContext) p;

                if (regCtx != a.rd)
                {
                    return false;
                }

                rdText = a.rd.getText();
                op2 = a.op2();

                if (op2 == null || op2.register() == null)
                {
                    return false;
                }

                return rdText != null && rdText.equalsIgnoreCase(op2.register().getText());
            }

            if (p instanceof LinterParser.ShiftLongContext)
            {
                LinterParser.ShiftLongContext s;

                s = (LinterParser.ShiftLongContext) p;

                if (regCtx != s.rd)
                {
                    return false;
                }

                rdText = s.rd.getText();

                return rdText != null && rdText.equalsIgnoreCase(s.rn.getText());
            }

            if (p instanceof LinterParser.ShiftShortContext)
            {
                LinterParser.ShiftShortContext s;
                LinterParser.Op2Context op2;

                s = (LinterParser.ShiftShortContext) p;

                if (regCtx != s.rd)
                {
                    return false;
                }

                rdText = s.rd.getText();
                op2 = s.rn;

                if (op2 == null || op2.register() == null)
                {
                    return false;
                }

                return rdText != null && rdText.equalsIgnoreCase(op2.register().getText());
            }

            p = p.getParent();
        }

        return false;
    }

    private String formatParamSet(boolean[] params)
    {
        StringBuilder sb;
        int i;
        boolean first;

        sb = new StringBuilder();
        first = true;

        for (i = 0; i < 4; i++)
        {
            if (params[i])
            {
                if (!first)
                {
                    sb.append(", ");
                }
                sb.append("r").append(i);
                first = false;
            }
        }

        if (sb.length() == 0)
        {
            return "(none)";
        }

        return sb.toString();
    }

    private int highestParam(boolean[] params)
    {
        int i;
        int highest;

        highest = -1;

        for (i = 0; i < 4; i++)
        {
            if (params[i])
            {
                highest = i;
            }
        }

        return highest;
    }

    private String formatExpectedPrefix(boolean[] params)
    {
        int highest;
        int i;
        StringBuilder sb;

        highest = highestParam(params);

        if (highest < 0)
        {
            return "(none)";
        }

        sb = new StringBuilder();

        for (i = 0; i <= highest; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append("r").append(i);
        }

        return sb.toString();
    }

    private String formatMissingFromPrefix(boolean[] params)
    {
        int highest;
        int i;
        StringBuilder sb;
        boolean first;

        highest = highestParam(params);

        if (highest < 1)
        {
            return "(none)";
        }

        sb = new StringBuilder();
        first = true;

        for (i = 0; i < highest; i++)
        {
            if (!params[i])
            {
                if (!first)
                {
                    sb.append(", ");
                }
                sb.append("r").append(i);
                first = false;
            }
        }

        if (sb.length() == 0)
        {
            return "(none)";
        }

        return sb.toString();
    }
}