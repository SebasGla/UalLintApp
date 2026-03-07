package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
 * Linting rule that catches multiplication or division by a power of two.
 * It suggests using bit-shifts (LSL, LSR, ASR) instead, which are faster and more efficient.
 * It uses a simple tracking system to remember which registers hold constants across a few lines.
 */
public final class ShiftPow2LintListener extends LinterParserBaseListener
{
    // How many instructions a constant "lives" before we forget it to avoid false positives
    private static final int CONST_TTL = 2;

    private final DiagnosticCollector diags;

    // Map of register names to their current known constant values
    private final Map<String, ConstBinding> constRegs = new HashMap<>();

    public ShiftPow2LintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    /*
     * Main hook for every instruction.
     * Handles the lifecycle of constants: checking usage, clearing overwrites, and learning new values.
     */
    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx)
    {
        decayConstants(); // Age all known constants by one step

        // 1) See if the current instruction is a MUL or DIV using a known power-of-two constant
        if (ctx.mulInstr() != null)
        {
            checkMul(ctx.mulInstr());
        }

        if (ctx.divInstr() != null)
        {
            checkDiv(ctx.divInstr());
        }

        // 2) If this instruction writes to a register, we must clear any old constant we had for it
        clobberWrittenRegister(ctx);

        // 3) Try to learn new constants from MOV, MOVW, or LDR instructions
        if (ctx.movInstr() != null)
        {
            learnFromMov(ctx.movInstr());
        }

        if (ctx.movwInstr() != null)
        {
            learnFromMovw(ctx.movwInstr());
        }

        if (ctx.ldrInstr() != null)
        {
            learnFromLdrLiteral(ctx.ldrInstr());
        }
    }

    // -------------------------
    // Checks
    // -------------------------

    /*
     * Analyzes multiplication instructions to see if either operand is a power of two.
     */
    private void checkMul(LinterParser.MulInstrContext mul)
    {
        if (!(mul instanceof LinterParser.MulLongContext))
            return;

        LinterParser.MulLongContext m = (LinterParser.MulLongContext) mul;

        Token opTok = m.getStart();
        String mnemonic = lower(opTok.getText());

        String rd = lower(m.rd.getText());
        String rn = lower(m.rn.getText());
        String rm = lower(m.rm.getText());

        // Check if the second operand (rm) is a known power of 2
        ConstBinding c = constRegs.get(rm);
        if (c != null && isPow2(c.value))
        {
            int sh = log2(c.value); // Convert value to shift amount (e.g., 4 -> 2)
            String suggested = "lsl " + rd + ", " + rn + ", #" + sh;

            diags.report(Rules.ShiftPow2, Severity.WARNING, opTok,
                    "multiplication by 2^n must use shifts. " +
                            "found `" + mnemonic + " " + rd + ", " + rn + ", " + rm + "` with " + rm + " = #" + c.value + ". " +
                            "use: `" + suggested + "`.");
            return;
        }

        // Also check rn since multiplication is commutative
        c = constRegs.get(rn);
        if (c != null && isPow2(c.value))
        {
            int sh = log2(c.value);
            String suggested = "lsl " + rd + ", " + rm + ", #" + sh;

            diags.report(Rules.ShiftPow2, Severity.WARNING, opTok,
                    "multiplication by 2^n must use shifts. " +
                            "found `" + mnemonic + " " + rd + ", " + rn + ", " + rm + "` with " + rn + " = #" + c.value + ". " +
                            "use: `" + suggested + "`.");
        }
    }

    /*
     * Analyzes division instructions to see if the divisor is a power of two.
     */
    private void checkDiv(LinterParser.DivInstrContext div)
    {
        Token opTok = div.getStart();
        String mnemonic = lower(opTok.getText());

        if (div.register().size() < 2)
            return;

        String rd = lower(div.register(0).getText());
        String rn = lower(div.register(1).getText());
        String rm;

        // DIV usually needs 3 operands to provide a clean "shift" suggestion
        if (div.register().size() >= 3)
            rm = lower(div.register(2).getText());
        else
            return;

        ConstBinding c = constRegs.get(rm);
        if (c == null)
            return;

        if (!isPow2(c.value))
            return;

        int sh = log2(c.value);

        // UDIV (Unsigned) maps directly to LSR (Logical Shift Right)
        if (mnemonic.startsWith("udiv"))
        {
            String suggested = "lsr " + rd + ", " + rn + ", #" + sh;

            diags.report(Rules.ShiftPow2, Severity.WARNING, opTok,
                    "division by 2^n must use shifts. " +
                            "found `" + mnemonic + " " + rd + ", " + rn + ", " + rm + "` with " + rm + " = #" + c.value + ". " +
                            "use: `" + suggested + "`.");
            return;
        }

        // SDIV (Signed) maps to ASR (Arithmetic Shift Right)
        if (mnemonic.startsWith("sdiv"))
        {
            String suggested = "asr " + rd + ", " + rn + ", #" + sh;

            diags.report(Rules.ShiftPow2, Severity.WARNING, opTok,
                    "division by 2^n must use shifts. " +
                            "found `" + mnemonic + " " + rd + ", " + rn + ", " + rm + "` with " + rm + " = #" + c.value + ". " +
                            "use: `" + suggested + "`. note: `asr` matches `sdiv` only if the dividend is non-negative.");
        }
    }

    // -------------------------
    // Learning constants
    // -------------------------

    /*
     * Extracts constants from MOV instructions (e.g., MOV R1, #4).
     */
    private void learnFromMov(LinterParser.MovInstrContext mov)
    {
        String rd = lower(mov.register().getText());

        Integer v = extractConstFromFlexop2(mov.flexop2());
        if (v == null)
            return;

        constRegs.put(rd, new ConstBinding(v, CONST_TTL));
    }

    /*
     * Extracts constants from MOVW (Move Wide) instructions.
     */
    private void learnFromMovw(LinterParser.MovwInstrContext movw)
    {
        String rd = lower(movw.register().getText());

        Integer v = extractConstFromImmediate(movw.immediate());
        if (v == null)
            return;

        constRegs.put(rd, new ConstBinding(v, CONST_TTL));
    }

    /*
     * Extracts constants from LDR literal loads (e.g., LDR R1, =4).
     */
    private void learnFromLdrLiteral(LinterParser.LdrInstrContext ldr)
    {
        if (ldr.nondwordOption() == null)
            return;

        if (ldr.nondwordOption().literal() == null)
            return;

        String rt = lower(ldr.register().getText());
        Integer v = evalConstExpr(ldr.nondwordOption().literal().constExpr());
        if (v == null)
            return;

        constRegs.put(rt, new ConstBinding(v, CONST_TTL));
    }

    // -------------------------
    // Clobbering / lifetime
    // -------------------------

    /*
     * Removes a register from our constant map if an instruction overwrites it.
     */
    private void clobberWrittenRegister(LinterParser.InstructionContext ctx)
    {
        String written = getWrittenRegister(ctx);
        if (written == null)
            return;

        constRegs.remove(written);
    }

    /*
     * Identifies which register an instruction writes to.
     */
    private static String getWrittenRegister(LinterParser.InstructionContext ctx)
    {
        if (ctx.movInstr() != null)
            return lower(ctx.movInstr().register().getText());

        if (ctx.movwInstr() != null)
            return lower(ctx.movwInstr().register().getText());

        if (ctx.ldrInstr() != null)
            return lower(ctx.ldrInstr().register().getText());

        if (ctx.mulInstr() != null)
        {
            if (ctx.mulInstr() instanceof LinterParser.MulLongContext)
                return lower(((LinterParser.MulLongContext) ctx.mulInstr()).rd.getText());
            if (ctx.mulInstr() instanceof LinterParser.MulShortContext)
                return lower(((LinterParser.MulShortContext) ctx.mulInstr()).rd.getText());
        }

        if (ctx.divInstr() != null && ctx.divInstr().register().size() >= 1)
            return lower(ctx.divInstr().register(0).getText());

        return null;
    }

    /*
     * Reduces the TTL of all active constant bindings.
     * If a constant isn't used within a few lines, we forget it.
     */
    private void decayConstants()
    {
        Iterator<Map.Entry<String, ConstBinding>> it = constRegs.entrySet().iterator();
        while (it.hasNext())
        {
            Map.Entry<String, ConstBinding> e = it.next();
            e.getValue().ttl--;
            if (e.getValue().ttl <= 0)
                it.remove();
        }
    }

    // -------------------------
    // Constant extraction / evaluation
    // -------------------------

    private static Integer extractConstFromFlexop2(LinterParser.Flexop2Context flex)
    {
        if (flex == null)
            return null;

        if (flex.op2() == null)
            return null;

        return extractConstFromOp2(flex.op2());
    }

    private static Integer extractConstFromOp2(LinterParser.Op2Context op2)
    {
        if (op2 == null)
            return null;

        if (op2.immediate() != null)
            return extractConstFromImmediate(op2.immediate());

        if (op2.constExpr() != null)
            return evalConstExpr(op2.constExpr());

        return null;
    }

    private static Integer extractConstFromImmediate(LinterParser.ImmediateContext imm)
    {
        if (imm == null)
            return null;

        if (imm instanceof LinterParser.ImmediateHashContext)
            return evalConstExpr(((LinterParser.ImmediateHashContext) imm).constExpr());

        if (imm instanceof LinterParser.ImmediateMissingHashContext)
            return evalConstExpr(((LinterParser.ImmediateMissingHashContext) imm).constExpr());

        return null;
    }

    /*
     * Recursively evaluates a constant expression (e.g., 2 + 2) into a numeric integer.
     * Returns null if the expression involves symbols or labels that we can't resolve.
     */
    private static Integer evalConstExpr(LinterParser.ConstExprContext ctx)
    {
        if (ctx == null)
            return null;

        if (ctx instanceof LinterParser.ConstAtomContext)
        {
            return evalConstPrimary(((LinterParser.ConstAtomContext) ctx).constPrimary());
        }

        if (ctx instanceof LinterParser.ConstUnaryContext)
        {
            LinterParser.ConstUnaryContext u = (LinterParser.ConstUnaryContext) ctx;
            Integer v = evalConstExpr(u.constExpr());
            if (v == null)
                return null;

            Token op = u.op;
            if (op == null)
                return null;

            String t = op.getText();
            if (t.equals("+")) return v;
            if (t.equals("-")) return -v;
            if (t.equals("~")) return ~v;

            return null;
        }

        if (ctx instanceof LinterParser.ConstMulContext)
        {
            return evalBinary(((LinterParser.ConstMulContext) ctx).op.getText(),
                    ((LinterParser.ConstMulContext) ctx).constExpr(0),
                    ((LinterParser.ConstMulContext) ctx).constExpr(1));
        }

        if (ctx instanceof LinterParser.ConstAddContext)
        {
            return evalBinary(((LinterParser.ConstAddContext) ctx).op.getText(),
                    ((LinterParser.ConstAddContext) ctx).constExpr(0),
                    ((LinterParser.ConstAddContext) ctx).constExpr(1));
        }

        if (ctx instanceof LinterParser.ConstShiftContext)
        {
            return evalBinary(((LinterParser.ConstShiftContext) ctx).op.getText(),
                    ((LinterParser.ConstShiftContext) ctx).constExpr(0),
                    ((LinterParser.ConstShiftContext) ctx).constExpr(1));
        }

        if (ctx instanceof LinterParser.ConstAndContext)
        {
            return evalBinary("&",
                    ((LinterParser.ConstAndContext) ctx).constExpr(0),
                    ((LinterParser.ConstAndContext) ctx).constExpr(1));
        }

        if (ctx instanceof LinterParser.ConstXorContext)
        {
            return evalBinary("^",
                    ((LinterParser.ConstXorContext) ctx).constExpr(0),
                    ((LinterParser.ConstXorContext) ctx).constExpr(1));
        }

        if (ctx instanceof LinterParser.ConstOrContext)
        {
            return evalBinary("|",
                    ((LinterParser.ConstOrContext) ctx).constExpr(0),
                    ((LinterParser.ConstOrContext) ctx).constExpr(1));
        }

        return null;
    }

    /*
     * Base case for constant evaluation: parses raw numbers in decimal, hex, or binary.
     */
    private static Integer evalConstPrimary(LinterParser.ConstPrimaryContext p)
    {
        if (p == null)
            return null;

        if (p.INT() != null)
            return parseIntSafe(p.INT().getText(), 10);

        if (p.INT_HEX() != null)
        {
            String s = p.INT_HEX().getText();
            s = s.substring(2); // Strip the 0x prefix
            return parseIntSafe(s, 16);
        }

        if (p.INT_BIN() != null)
        {
            String s = p.INT_BIN().getText();
            s = s.substring(2); // Strip the 0b prefix
            return parseIntSafe(s, 2);
        }

        if (p.LPAREN() != null && p.constExpr() != null)
            return evalConstExpr(p.constExpr());

        return null;
    }

    private static Integer evalBinary(String op, LinterParser.ConstExprContext a, LinterParser.ConstExprContext b)
    {
        Integer x = evalConstExpr(a);
        Integer y = evalConstExpr(b);

        if (x == null || y == null)
            return null;

        try
        {
            if (op.equals("*")) return x * y;
            if (op.equals("/")) return y == 0 ? null : x / y;
            if (op.equals("%")) return y == 0 ? null : x % y;
            if (op.equals("+")) return x + y;
            if (op.equals("-")) return x - y;
            if (op.equals("<<")) return x << y;
            if (op.equals(">>")) return x >> y;
            if (op.equals("&")) return x & y;
            if (op.equals("^")) return x ^ y;
            if (op.equals("|")) return x | y;
        }
        catch (ArithmeticException ex)
        {
            return null;
        }

        return null;
    }

    // -------------------------
    // Math helpers
    // -------------------------

    private static boolean isPow2(int v)
    {
        if (v <= 0)
            return false;
        return (v & (v - 1)) == 0;
    }

    private static int log2(int v)
    {
        int n = 0;
        while (v > 1)
        {
            v >>= 1;
            n++;
        }
        return n;
    }

    private static Integer parseIntSafe(String s, int radix)
    {
        try
        {
            return Integer.parseInt(s, radix);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

    private static String lower(String s)
    {
        if (s == null)
            return "";
        return s.toLowerCase();
    }

    /*
     * Internal container to hold a register's constant value and its remaining "life" (TTL).
     */
    private static final class ConstBinding
    {
        private final int value;
        private int ttl;

        private ConstBinding(int value, int ttl)
        {
            this.value = value;
            this.ttl = ttl;
        }
    }
}