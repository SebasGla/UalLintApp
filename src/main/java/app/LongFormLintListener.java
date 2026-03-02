package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

/**
 * Rule: LONGFORM
 * Only allow the long instruction form.
 *
 * Short form means the assembler-implied "Rn == Rd" variant, e.g.:
 *   orr r1, r2        (short)  -> illegal by policy
 *   orr r1, r1, r2    (long)   -> legal
 *
 * The updated grammar encodes this via labeled alternatives:
 *  - arithmeticInstr: #ArithmShort
 *  - mulInstr:        #MulShort
 *  - shiftInstruction:#ShiftShort
 *  - logicInstr:      #LogicShort
 */
public final class LongFormLintListener extends LinterParserBaseListener
{
    private final DiagnosticCollector diags;

    public LongFormLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx)
    {
        if (ctx.arithmeticInstr() != null && ctx.arithmeticInstr() instanceof LinterParser.ArithmShortContext)
        {
            reportArithmShort((LinterParser.ArithmShortContext) ctx.arithmeticInstr());
            return;
        }

        if (ctx.logicInstr() != null && ctx.logicInstr() instanceof LinterParser.LogicShortContext)
        {
            reportLogicShort((LinterParser.LogicShortContext) ctx.logicInstr());
            return;
        }

        if (ctx.shiftInstruction() != null && ctx.shiftInstruction() instanceof LinterParser.ShiftShortContext)
        {
            reportShiftShort((LinterParser.ShiftShortContext) ctx.shiftInstruction());
            return;
        }

        if (ctx.mulInstr() != null && ctx.mulInstr() instanceof LinterParser.MulShortContext)
        {
            reportMulShort((LinterParser.MulShortContext) ctx.mulInstr());
            return;
        }
    }

    // -------------------------
    // Reporting (per short form)
    // -------------------------

    private void reportArithmShort(LinterParser.ArithmShortContext ctx)
    {
        Token opcode = ctx.getStart();
        String mnemonic = toLower(opcode.getText());

        String rd = ctx.rd.getText();
        String op2 = ctx.op2().getText();

        String suggested = mnemonic + " " + rd + ", " + rd + ", " + op2;

        report(opcode, "Short form is not allowed for arithmetic instruction. Use long form: `" + suggested + "`.");
    }

    private void reportLogicShort(LinterParser.LogicShortContext ctx)
    {
        Token opcode = ctx.getStart();
        String mnemonic = toLower(opcode.getText());

        String rd = ctx.rd.getText();
        String rn = ctx.rn.getText(); // rn is op2 in the short rule

        String suggested = mnemonic + " " + rd + ", " + rd + ", " + rn;

        report(opcode, "Short form is not allowed for logic instruction. Use long form: `" + suggested + "`.");
    }

    private void reportShiftShort(LinterParser.ShiftShortContext ctx)
    {
        Token opcode = ctx.getStart();
        String mnemonic = toLower(opcode.getText());

        String rd = ctx.rd.getText();
        String rn = ctx.rn.getText(); // rn is op2 in the short rule

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
