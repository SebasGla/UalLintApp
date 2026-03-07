package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

/*
 * Linting rule to enforce the use of the "long" instruction form.
 * * Assembly allows shorthand (e.g., "ADD R1, R2") which implicitly means
 * "ADD R1, R1, R2". This rule bans the short form to ensure code is
 * completely explicit and easier to read.
 * * It relies heavily on ANTLR's rule labels (like #ArithmShort) to easily
 * distinguish between the short and long grammar branches.
 */
public final class LongFormLintListener extends LinterParserBaseListener
{
    private final DiagnosticCollector diags;

    public LongFormLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    /*
     * Intercepts every parsed instruction and checks if it falls into one of
     * the "short" grammar categories defined in the .g4 file.
     */
    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx)
    {
        // Check if it's an arithmetic instruction that hit the #ArithmShort branch
        if (ctx.arithmeticInstr() != null && ctx.arithmeticInstr() instanceof LinterParser.ArithmShortContext)
        {
            reportArithmShort((LinterParser.ArithmShortContext) ctx.arithmeticInstr());
            return;
        }

        // Check if it's a logical instruction that hit the #LogicShort branch
        if (ctx.logicInstr() != null && ctx.logicInstr() instanceof LinterParser.LogicShortContext)
        {
            reportLogicShort((LinterParser.LogicShortContext) ctx.logicInstr());
            return;
        }

        // Check if it's a shift instruction that hit the #ShiftShort branch
        if (ctx.shiftInstruction() != null && ctx.shiftInstruction() instanceof LinterParser.ShiftShortContext)
        {
            reportShiftShort((LinterParser.ShiftShortContext) ctx.shiftInstruction());
            return;
        }

        // Check if it's a multiply instruction that hit the #MulShort branch
        if (ctx.mulInstr() != null && ctx.mulInstr() instanceof LinterParser.MulShortContext)
        {
            reportMulShort((LinterParser.MulShortContext) ctx.mulInstr());
            return;
        }
    }

    // -------------------------
    // Reporting (per short form)
    // -------------------------

    /*
     * Formats the warning for short arithmetic instructions.
     * It actively reconstructs what the code *should* look like (the suggested long form)
     * so the user knows exactly how to fix it.
     */
    private void reportArithmShort(LinterParser.ArithmShortContext ctx)
    {
        Token opcode = ctx.getStart();
        String mnemonic = toLower(opcode.getText());

        // Grab the destination register and the second operand
        String rd = ctx.rd.getText();
        String op2 = ctx.op2().getText();

        // Build the suggested fix: "add r1, r1, r2"
        String suggested = mnemonic + " " + rd + ", " + rd + ", " + op2;

        report(opcode, "Short form is not allowed for arithmetic instruction. Use long form: `" + suggested + "`.");
    }

    private void reportLogicShort(LinterParser.LogicShortContext ctx)
    {
        Token opcode = ctx.getStart();
        String mnemonic = toLower(opcode.getText());

        String rd = ctx.rd.getText();
        String rn = ctx.rn.getText(); // In the short logic rule, 'rn' acts as the second operand

        String suggested = mnemonic + " " + rd + ", " + rd + ", " + rn;

        report(opcode, "Short form is not allowed for logic instruction. Use long form: `" + suggested + "`.");
    }

    private void reportShiftShort(LinterParser.ShiftShortContext ctx)
    {
        Token opcode = ctx.getStart();
        String mnemonic = toLower(opcode.getText());

        String rd = ctx.rd.getText();
        String rn = ctx.rn.getText();

        String suggested = mnemonic + " " + rd + ", " + rd + ", " + rn;

        report(opcode, "Short form is not allowed for shift instruction. Use long form: `" + suggested + "`.");
    }

    private void reportMulShort(LinterParser.MulShortContext ctx)
    {
        Token opcode = ctx.getStart();
        String mnemonic = toLower(opcode.getText());

        String rd = ctx.rd.getText();
        String rn = ctx.rn.getText();

        String suggested = mnemonic + " " + rd + ", " + rd + ", " + rn;

        report(opcode, "Short form is not allowed for multiply instruction. Use long form: `" + suggested + "`.");
    }

    private void report(Token where, String msg)
    {
        diags.report(Rules.ShortInstructionForm, Severity.WARNING, where, msg);
    }

    private static String toLower(String s)
    {
        if (s == null)
            return "";
        return s.toLowerCase();
    }
}