package app;

import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.Token;

/*
 * Linting rule to catch arithmetic operations being used for bitwise/logical tasks.
 * For example, adding a power of two (add r0, #4) instead of OR-ing it (orr r0, #4),
 * or multiplying by a power of two (mul) instead of just bit-shifting (lsl).
 */
public final class LogicalOpLintListener extends LinterParserBaseListener
{
    private static final Rules ruleId = Rules.LogicalOp;

    private final DiagnosticCollector diags;

    // Track state to catch multi-line operations (e.g., MOV followed by MUL)
    private int lastMaskMovLine;     // The line number of the last MOV instruction
    private String lastMaskMovReg;   // Which register got loaded with the mask
    private Long lastMaskMovValue;   // The actual numeric value of the mask

    public LogicalOpLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
        this.lastMaskMovLine = -1;
        this.lastMaskMovReg = null;
        this.lastMaskMovValue = null;
    }

    /*
     * Hooks into MOV instructions. We want to see if a developer is loading a
     * specific bitmask (like 0x01 or 0xFFFFFFFF) into a register so we can
     * check what they do with it on the very next line.
     */
    @Override
    public void exitMovInstr(LinterParser.MovInstrContext ctx)
    {
        LinterParser.RegisterContext rd;
        LinterParser.Flexop2Context flex;
        LinterParser.Op2Context op2;
        Long v;

        rd = ctx.register();

        if (rd == null)
        {
            clearLastMaskMov();
            return;
        }

        flex = ctx.flexop2();

        if (flex == null)
        {
            clearLastMaskMov();
            return;
        }

        op2 = flex.op2();

        if (op2 == null)
        {
            clearLastMaskMov();
            return;
        }

        // Try to figure out the actual number being moved
        v = extractConstValue(op2);

        if (v == null)
        {
            clearLastMaskMov();
            return;
        }

        // Only care if it looks like a bitmask (powers of 2, or all 1s like 0xFF)
        if (!isMaskLike(v))
        {
            clearLastMaskMov();
            return;
        }

        // Save the state for the next instruction to check
        this.lastMaskMovLine = ctx.getStart().getLine();
        this.lastMaskMovReg = rd.getText();
        this.lastMaskMovValue = v;
    }

    /*
     * Checks standard ADD/SUB instructions with a full 3-operand list.
     * Looks for immediate values that are powers of 2.
     */
    @Override
    public void exitArithmLong(LinterParser.ArithmLongContext ctx)
    {
        LinterParser.Flexop2Context flex;
        LinterParser.Op2Context op2;
        Long immValue;

        flex = ctx.flexop2();

        if (flex == null)
        {
            return;
        }

        op2 = flex.op2();

        if (op2 == null)
        {
            return;
        }

        immValue = extractPow2ImmediateExceptOne(op2);

        if (immValue == null)
        {
            return;
        }

        reportAddSub(ctx.op, ctx.getStart(), immValue);
    }

    /*
     * Checks short ADD/SUB instructions (e.g., ADD R0, #4).
     */
    @Override
    public void exitArithmShort(LinterParser.ArithmShortContext ctx)
    {
        LinterParser.Op2Context op2;
        Long immValue;

        op2 = ctx.op2();

        if (op2 == null)
        {
            return;
        }

        immValue = extractPow2ImmediateExceptOne(op2);

        if (immValue == null)
        {
            return;
        }

        reportAddSub(ctx.op, ctx.getStart(), immValue);
    }

    /*
     * Checks if a long multiplication uses the mask we tracked from the previous line.
     */
    @Override
    public void exitMulLong(LinterParser.MulLongContext ctx)
    {
        int line;

        line = ctx.getStart().getLine();

        if (!isMovMaskImmediatelyBefore(line))
        {
            return;
        }

        if (!usesAsSourceMulLong(ctx, this.lastMaskMovReg))
        {
            return;
        }

        reportMulOrDiv(ctx.getStart(), "mul");
    }

    /*
     * Checks if a short multiplication uses the tracked mask.
     */
    @Override
    public void exitMulShort(LinterParser.MulShortContext ctx)
    {
        int line;

        line = ctx.getStart().getLine();

        if (!isMovMaskImmediatelyBefore(line))
        {
            return;
        }

        if (!usesAsSourceMulShort(ctx, this.lastMaskMovReg))
        {
            return;
        }

        reportMulOrDiv(ctx.getStart(), "mul");
    }

    /*
     * Checks if a division instruction uses the tracked mask.
     */
    @Override
    public void exitDivInstr(LinterParser.DivInstrContext ctx)
    {
        int line;

        line = ctx.getStart().getLine();

        if (!isMovMaskImmediatelyBefore(line))
        {
            return;
        }

        if (!usesAsSourceDiv(ctx, this.lastMaskMovReg))
        {
            return;
        }

        reportMulOrDiv(ctx.getStart(), "div");
    }

    /*
     * Generates the actual warning for using ADD or SUB with a power of 2.
     * Suggests ORR or BIC instead.
     */
    private void reportAddSub(Token opToken, Token atToken, long immValue)
    {
        String opText;
        String opLower;

        if (opToken == null)
        {
            return;
        }

        opText = opToken.getText();

        if (opText == null)
        {
            return;
        }

        opLower = opText.toLowerCase();

        if (opLower.startsWith("add"))
        {
            diags.report(
                    ruleId,
                    Severity.WARNING,
                    atToken,
                    "Arithmetic used with a power-of-two mask (+" + immValue + "); prefer ORR for logical intent."
            );

            return;
        }

        if (opLower.startsWith("sub"))
        {
            diags.report(
                    ruleId,
                    Severity.WARNING,
                    atToken,
                    "Arithmetic used with a power-of-two mask (-" + immValue + "); prefer BIC for logical intent."
            );

            return;
        }
    }

    /*
     * Generates warnings for using MUL or DIV with a mask.
     * Suggests LSL/LSR/ASR (shifts) or AND/BIC depending on the mask shape.
     */
    private void reportMulOrDiv(Token atToken, String kind)
    {
        long v;

        if (this.lastMaskMovValue == null)
        {
            return;
        }

        v = this.lastMaskMovValue.longValue();

        // If it's a straight power of 2 (e.g., 4), they should be bit-shifting
        if (isPow2(v))
        {
            if (kind.equals("mul"))
            {
                diags.report(
                        ruleId,
                        Severity.WARNING,
                        atToken,
                        "Suspicious mask constant loaded in previous line (2^n = " + v + ") is used with MUL; prefer LSL/LSR shifts."
                );
            }
            else
            {
                diags.report(
                        ruleId,
                        Severity.WARNING,
                        atToken,
                        "Suspicious mask constant loaded in previous line (2^n = " + v + ") is used with DIV; prefer LSR/ASR shifts."
                );
            }

            return;
        }

        // If it's a block of 1s (e.g., 0xFF), they should probably be doing a bitwise AND/BIC
        if (isPow2Minus1(v))
        {
            diags.report(
                    ruleId,
                    Severity.WARNING,
                    atToken,
                    "Mask-like constant loaded in previous line (2^n-1 = " + v + ") is used with " + kind.toUpperCase()
                            + "; if you intended masking, prefer AND/BIC instead of arithmetic."
            );

            return;
        }
    }

    /*
     * Checks if the tracked MOV instruction happened exactly one line prior to the current line.
     */
    private boolean isMovMaskImmediatelyBefore(int currentLine)
    {
        if (this.lastMaskMovLine < 0)
        {
            return false;
        }

        if (this.lastMaskMovReg == null)
        {
            return false;
        }

        return this.lastMaskMovLine == (currentLine - 1);
    }

    /*
     * Checks if the target register was used as an input for a long MUL instruction.
     */
    private static boolean usesAsSourceMulLong(LinterParser.MulLongContext ctx, String reg)
    {
        String rn;
        String rm;

        if (ctx == null || reg == null)
        {
            return false;
        }

        rn = textOf(ctx.rn);
        rm = textOf(ctx.rm);

        return regEquals(rn, reg) || regEquals(rm, reg);
    }

    /*
     * Checks if the target register was used as an input for a short MUL instruction.
     */
    private static boolean usesAsSourceMulShort(LinterParser.MulShortContext ctx, String reg)
    {
        String rn;

        if (ctx == null || reg == null)
        {
            return false;
        }

        rn = textOf(ctx.rn);

        return regEquals(rn, reg);
    }

    /*
     * Checks if the target register was used as an input for a DIV instruction.
     */
    private static boolean usesAsSourceDiv(LinterParser.DivInstrContext ctx, String reg)
    {
        int n;
        int i;

        if (ctx == null || reg == null)
        {
            return false;
        }

        n = ctx.getRuleContexts(LinterParser.RegisterContext.class).size();

        if (n < 2)
        {
            return false;
        }

        i = 1;

        // Loop through all registers (except the first one, which is the destination)
        while (i < n)
        {
            LinterParser.RegisterContext r;

            r = ctx.getRuleContext(LinterParser.RegisterContext.class, i);

            if (r != null && regEquals(r.getText(), reg))
            {
                return true;
            }

            i = i + 1;
        }

        return false;
    }

    /*
     * Extracts an immediate number, but ignores 1.
     * (Adding/subtracting 1 is normal incrementing, not usually a bitmask operation).
     */
    private static Long extractPow2ImmediateExceptOne(LinterParser.Op2Context op2)
    {
        Long v;

        v = extractConstValue(op2);

        if (v == null)
        {
            return null;
        }

        if (v.longValue() == 1L)
        {
            return null;
        }

        if (!isPow2(v))
        {
            return null;
        }

        return v;
    }

    /*
     * Digs into the ANTLR context to extract the mathematical expression tree.
     */
    private static Long extractConstValue(LinterParser.Op2Context op2)
    {
        LinterParser.ConstExprContext expr;
        Long v;

        if (op2 == null)
        {
            return null;
        }

        expr = null;

        if (op2.immediate() != null)
        {
            expr = op2.immediate().getRuleContext(LinterParser.ConstExprContext.class, 0);
        }
        else if (op2.constExpr() != null)
        {
            expr = op2.constExpr();
        }
        else
        {
            return null;
        }

        if (expr == null)
        {
            return null;
        }

        v = evalConstExpr(expr);

        return v;
    }

    /*
     * Validates if a number is a power of 2, or one less than a power of 2 (e.g., 0xFF).
     */
    private static boolean isMaskLike(long v)
    {
        if (v <= 0)
        {
            return false;
        }

        return isPow2(v) || isPow2Minus1(v);
    }

    /*
     * Classic bit-twiddling trick to check if a number is a power of 2.
     */
    private static boolean isPow2(long v)
    {
        if (v <= 0)
        {
            return false;
        }

        return (v & (v - 1)) == 0;
    }

    /*
     * Checks if a number consists of all 1s up to a point (like 7, 15, 255).
     */
    private static boolean isPow2Minus1(long v)
    {
        if (v <= 0)
        {
            return false;
        }

        if (v == 0xffffffffL)
        {
            return true;
        }

        return isPow2(v + 1);
    }

    private void clearLastMaskMov()
    {
        this.lastMaskMovLine = -1;
        this.lastMaskMovReg = null;
        this.lastMaskMovValue = null;
    }

    private static String textOf(ParserRuleContext ctx)
    {
        if (ctx == null)
        {
            return null;
        }

        return ctx.getText();
    }

    private static boolean regEquals(String a, String b)
    {
        if (a == null || b == null)
        {
            return false;
        }

        return a.equalsIgnoreCase(b);
    }

    /*
     * Recursively walks down the ANTLR expression tree to calculate the final
     * mathematical result of a constant expression (e.g., evaluating "4 + 8" to 12).
     */
    private static Long evalConstExpr(LinterParser.ConstExprContext ctx)
    {
        if (ctx == null)
        {
            return null;
        }

        if (ctx instanceof LinterParser.ConstAtomContext atom)
        {
            return evalConstPrimary(atom.constPrimary());
        }

        if (ctx instanceof LinterParser.ConstUnaryContext un)
        {
            Long rhs;

            rhs = evalConstExpr(un.constExpr());

            if (rhs == null)
            {
                return null;
            }

            switch (un.op.getType())
            {
                case LinterParser.PLUS:
                {
                    return rhs;
                }
                case LinterParser.MINUS:
                {
                    return -rhs;
                }
                case LinterParser.TILDE:
                {
                    return ~rhs;
                }
                default:
                {
                    return null;
                }
            }
        }

        if (ctx instanceof LinterParser.ConstAddContext add)
        {
            Long a;
            Long b;

            a = evalConstExpr(add.constExpr(0));
            b = evalConstExpr(add.constExpr(1));

            if (a == null || b == null)
            {
                return null;
            }

            if (add.op.getType() == LinterParser.PLUS)
            {
                return a + b;
            }

            if (add.op.getType() == LinterParser.MINUS)
            {
                return a - b;
            }

            return null;
        }

        if (ctx instanceof LinterParser.ConstMulContext mul)
        {
            Long a;
            Long b;

            a = evalConstExpr(mul.constExpr(0));
            b = evalConstExpr(mul.constExpr(1));

            if (a == null || b == null)
            {
                return null;
            }

            switch (mul.op.getType())
            {
                case LinterParser.STAR:
                {
                    return a * b;
                }
                case LinterParser.SLASH:
                {
                    if (b == 0)
                    {
                        return null; // Protect against divide-by-zero crashes
                    }

                    return a / b;
                }
                case LinterParser.MOD:
                {
                    if (b == 0)
                    {
                        return null;
                    }

                    return a % b;
                }
                default:
                {
                    return null;
                }
            }
        }

        if (ctx instanceof LinterParser.ConstShiftContext sh)
        {
            Long a;
            Long b;
            int shift;

            a = evalConstExpr(sh.constExpr(0));
            b = evalConstExpr(sh.constExpr(1));

            if (a == null || b == null)
            {
                return null;
            }

            if (b < 0 || b > 63)
            {
                return null;
            }

            shift = (int) (long) b;

            if (sh.op.getType() == LinterParser.LSHIFT)
            {
                return a << shift;
            }

            if (sh.op.getType() == LinterParser.RSHIFT)
            {
                return a >> shift;
            }

            return null;
        }

        if (ctx instanceof LinterParser.ConstAndContext and)
        {
            Long a;
            Long b;

            a = evalConstExpr(and.constExpr(0));
            b = evalConstExpr(and.constExpr(1));

            if (a == null || b == null)
            {
                return null;
            }

            return a & b;
        }

        if (ctx instanceof LinterParser.ConstOrContext or)
        {
            Long a;
            Long b;

            a = evalConstExpr(or.constExpr(0));
            b = evalConstExpr(or.constExpr(1));

            if (a == null || b == null)
            {
                return null;
            }

            return a | b;
        }

        if (ctx instanceof LinterParser.ConstXorContext xor)
        {
            Long a;
            Long b;

            a = evalConstExpr(xor.constExpr(0));
            b = evalConstExpr(xor.constExpr(1));

            if (a == null || b == null)
            {
                return null;
            }

            return a ^ b;
        }

        return null;
    }

    /*
     * The base case for evalConstExpr. Converts raw text numbers (hex, bin, dec)
     * into Longs, or recursively dives into parentheses.
     */
    private static Long evalConstPrimary(LinterParser.ConstPrimaryContext p)
    {
        String t;

        if (p == null)
        {
            return null;
        }

        if (p.INT() != null)
        {
            t = p.INT().getText();

            return parseLongSafe(t, 10);
        }

        if (p.INT_HEX() != null)
        {
            t = p.INT_HEX().getText();

            if (t == null)
            {
                return null;
            }

            t = t.toLowerCase();

            if (t.startsWith("0x"))
            {
                t = t.substring(2);
            }

            return parseLongSafe(t, 16);
        }

        if (p.INT_BIN() != null)
        {
            t = p.INT_BIN().getText();

            if (t == null)
            {
                return null;
            }

            t = t.toLowerCase();

            if (t.startsWith("0b"))
            {
                t = t.substring(2);
            }

            return parseLongSafe(t, 2);
        }

        if (p.LPAREN() != null)
        {
            return evalConstExpr(p.constExpr());
        }

        return null;
    }

    /*
     * Wrapper to safely parse strings into numbers without throwing exceptions
     * that would crash the whole walker.
     */
    private static Long parseLongSafe(String s, int radix)
    {
        try
        {
            if (s == null)
            {
                return null;
            }

            return Long.parseLong(s, radix);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}