package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Rule: SHIFTPOW2
 * Multiplication/division by 2^n must use shifts.
 *
 * Implementation:
 * - Track simple constant bindings: a register is known to hold an integer constant
 *   if it is defined by mov/movw/ldr-literal with a fully evaluable constExpr.
 * - If mul / udiv / sdiv uses such a register as multiplier/divisor and that constant is 2^n,
 *   report and propose the equivalent shift.
 *
 * Limitations (intentional):
 * - No interprocedural or full dataflow tracking.
 * - Constant bindings expire after a small TTL to reduce false positives.
 */
public final class ShiftPow2LintListener extends LinterParserBaseListener
{
    private static final int CONST_TTL = 2;

    private final DiagnosticCollector diags;
    private final Map<String, ConstBinding> constRegs = new HashMap<>();

    public ShiftPow2LintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx)
    {
        decayConstants();

        // 1) check for violations using current environment
        if (ctx.mulInstr() != null)
        {
            checkMul(ctx.mulInstr());
        }

        if (ctx.divInstr() != null)
        {
            checkDiv(ctx.divInstr());
        }

        // 2) clobber first (kills previous binding)
        clobberWrittenRegister(ctx);

        // 3) then learn from the instruction (creates new binding)
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

        // Prefer rm as the scaling factor
        ConstBinding c = constRegs.get(rm);
        if (c != null && isPow2(c.value))
        {
            int sh = log2(c.value);
            String suggested = "lsl " + rd + ", " + rn + ", #" + sh;

            diags.report(Rules.ShiftPow2, Severity.WARNING, opTok,
                    "multiplication by 2^n must use shifts. " +
                            "found `" + mnemonic + " " + rd + ", " + rn + ", " + rm + "` with " + rm + " = #" + c.value + ". " +
                            "use: `" + suggested + "`.");
            return;
        }

        // Also check rn (mul is commutative)
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

    private void checkDiv(LinterParser.DivInstrContext div)
    {
        // divInstr: DIV register COMMA register (COMMA register)?;
        Token opTok = div.getStart();
        String mnemonic = lower(opTok.getText());

        if (div.register().size() < 2)
            return;

        String rd = lower(div.register(0).getText());
        String rn = lower(div.register(1).getText());
        String rm;

        if (div.register().size() >= 3)
            rm = lower(div.register(2).getText());
        else
            return; // require 3-operand form for a clear suggestion

        ConstBinding c = constRegs.get(rm);
        if (c == null)
            return;

        if (!isPow2(c.value))
            return;

        int sh = log2(c.value);

        if (mnemonic.startsWith("udiv"))
        {
            String suggested = "lsr " + rd + ", " + rn + ", #" + sh;

            diags.report(Rules.ShiftPow2, Severity.WARNING, opTok,
                    "division by 2^n must use shifts. " +
                            "found `" + mnemonic + " " + rd + ", " + rn + ", " + rm + "` with " + rm + " = #" + c.value + ". " +
                            "use: `" + suggested + "`.");
            return;
        }

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

    private void learnFromMov(LinterParser.MovInstrContext mov)
    {
        // movInstr: MOV register COMMA flexop2;
        // Learn only: mov rd, immediate (hash or missing-hash) or mov rd, constExpr
        String rd = lower(mov.register().getText());

        Integer v = extractConstFromFlexop2(mov.flexop2());
        if (v == null)
            return;

        constRegs.put(rd, new ConstBinding(v, CONST_TTL));
    }

    private void learnFromMovw(LinterParser.MovwInstrContext movw)
    {
        // movwInstr: MOVW register COMMA immediate;
        String rd = lower(movw.register().getText());

        Integer v = extractConstFromImmediate(movw.immediate());
        if (v == null)
            return;

        constRegs.put(rd, new ConstBinding(v, CONST_TTL));
    }

    private void learnFromLdrLiteral(LinterParser.LdrInstrContext ldr)
    {
        // ldrInstr: LDR register COMMA (nondwordOption | ...);
        // nondwordOption: (literal | immediate)
        // literal: ASSIGN constExpr;     // ldr rt, =constExpr
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

    private void clobberWrittenRegister(LinterParser.InstructionContext ctx)
    {
        String written = getWrittenRegister(ctx);
        if (written == null)
            return;

        constRegs.remove(written);
    }

    private static String getWrittenRegister(LinterParser.InstructionContext ctx)
    {
        // Minimal conservative set; expand if needed.
        if (ctx.movInstr() != null)
            return lower(ctx.movInstr().register().getText());

        if (ctx.movwInstr() != null)
            return lower(ctx.movwInstr().register().getText());

        if (ctx.ldrInstr() != null)
            return lower(ctx.ldrInstr().register().getText());

        if (ctx.mulInstr() != null)
        {
            // mulInstr: op=MUL rd=register ...
            if (ctx.mulInstr() instanceof LinterParser.MulLongContext)
                return lower(((LinterParser.MulLongContext) ctx.mulInstr()).rd.getText());
            if (ctx.mulInstr() instanceof LinterParser.MulShortContext)
                return lower(((LinterParser.MulShortContext) ctx.mulInstr()).rd.getText());
        }

        if (ctx.divInstr() != null && ctx.divInstr().register().size() >= 1)
            return lower(ctx.divInstr().register(0).getText());

        return null;
    }

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
    // Constant extraction / evaluation (based on your grammar)
    // -------------------------

    private static Integer extractConstFromFlexop2(LinterParser.Flexop2Context flex)
    {
        if (flex == null)
            return null;

        // flexop2: op2 | (register (COMMA shiftOperand))?;
        // We only accept the op2 branch, and only if it evaluates to a numeric constant.
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

        // immediate: HASH constExpr #ImmediateHash | constExpr #ImmediateMissingHash ;
        if (imm instanceof LinterParser.ImmediateHashContext)
            return evalConstExpr(((LinterParser.ImmediateHashContext) imm).constExpr());

        if (imm instanceof LinterParser.ImmediateMissingHashContext)
            return evalConstExpr(((LinterParser.ImmediateMissingHashContext) imm).constExpr());

        return null;
    }

    /**
     * Evaluates constExpr if it is purely numeric (INT/INT_HEX/INT_BIN and operations).
     * If any ID/STRING is involved, returns null.
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
            if (t.equals("+"))
                return v;
            if (t.equals("-"))
                return -v;
            if (t.equals("~"))
                return ~v;

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

        // Equality/relational exist in grammar; not meaningful for constant values in this rule.
        return null;
    }

    private static Integer evalConstPrimary(LinterParser.ConstPrimaryContext p)
    {
        if (p == null)
            return null;

        if (p.INT() != null)
            return parseIntSafe(p.INT().getText(), 10);

        if (p.INT_HEX() != null)
        {
            String s = p.INT_HEX().getText();
            s = s.substring(2); // strip 0x / 0X
            return parseIntSafe(s, 16);
        }

        if (p.INT_BIN() != null)
        {
            String s = p.INT_BIN().getText();
            s = s.substring(2); // strip 0b / 0B
            return parseIntSafe(s, 2);
        }

        if (p.LPAREN() != null && p.constExpr() != null)
            return evalConstExpr(p.constExpr());

        // ID / STRING => not evaluable
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
