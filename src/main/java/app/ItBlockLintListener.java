package app;

import org.antlr.v4.runtime.Token;
import antlr4gen.*;

public final class ItBlockLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.ItBlock;

    private final DiagnosticCollector diags;

    /** Number of *remaining* instructions in the current IT-block. */
    private int itRemaining = 0;

    public ItBlockLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    // ---------------------------
    // 1) Trigger on IT instruction
    // ---------------------------

    @Override
    public void exitItInstr(LinterParser.ItInstrContext ctx) {
        // Your grammar: itInstr: IT COND;
        // Lexer: IT: 'it' ('t'|'e')? ('t'|'e')? ('t'|'e')?
        // Examples: "it" => 1 instr, "itt"/"ite" => 2, "ittt"/"itte"/... => 3, "itttt" => 4
        String itText = ctx.IT().getText().toLowerCase(); // caseInsensitive lexer, but normalize anyway
        int blockLen = Math.max(1, itText.length() - 1);  // "it" length 2 => 1

        // Overwrite state (nested IT is invalid in real Thumb, but grammar allows; you can add a separate rule later)
        itRemaining = blockLen;
    }

    // --------------------------------------------
    // 2) For every instruction, check IT-block body
    // --------------------------------------------

    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx) {
        // The IT instruction itself should not be treated as "inside an IT-block body".
        if (ctx.itInstr() != null) {
            return;
        }

        if (itRemaining <= 0) {
            return;
        }

        // We are inside the IT-block body (next 1..4 instructions after IT).
        // Rule 4: No conditional branch in IT-block.
        Token offending = getConditionalBranchToken(ctx);
        if (offending != null)
        {
            diags.report(ruleId, Severity.ERROR, offending, "Conditional branch must not appear inside an IT block.");
        }

        // Consume one slot from the IT-block
        itRemaining--;
    }

    /**
     * Returns the offending token if the given instruction is a *conditional* branch
     * according to your grammar/tokens; otherwise returns null.
     */
    private static Token getConditionalBranchToken(LinterParser.InstructionContext ctx)
    {

        // (A) cbz/cbnz are always conditional branches.
        if (ctx.cbzInstr() != null)
        {
            // cbzInstr: CBNZ register COMMA labelRef;
            // CBNZ token covers both 'cbz' and 'cbnz'
            return ctx.cbzInstr().getStart();
        }

        // (B) "branch" rule: BRANCH, BRANCHLINK, BRANCHLINKX, BRANCHX, BRANXJAZELLE
        if (ctx.branch() != null)
        {
            LinterParser.BranchContext b = ctx.branch();

            if (b.BRANCH() != null)
            {
                Token t = b.BRANCH().getSymbol();
                return isConditionalMnemonic(t, "b") ? t : null;
            }

            if (b.BRANCHLINK() != null)
            {
                Token t = b.BRANCHLINK().getSymbol();
                return isConditionalMnemonic(t, "bl") ? t : null;
            }

            if (b.BRANCHLINKX() != null)
            {
                Token t = b.BRANCHLINKX().getSymbol();
                return isConditionalMnemonic(t, "blx") ? t : null;
            }

            if (b.BRANCHX() != null)
            {
                Token t = b.BRANCHX().getSymbol();
                return isConditionalMnemonic(t, "bx") ? t : null;
            }

            if (b.BRANXJAZELLE() != null)
            {
                Token t = b.BRANXJAZELLE().getSymbol();
                return isConditionalMnemonic(t, "bxj") ? t : null;
            }
        }

        return null;
    }

    /**
     * Your lexer folds {cond} into the mnemonic token text (e.g. "bne", "blt", "bxge").
     * This function detects whether the mnemonic is conditional (i.e., has a {cond} suffix)
     * excluding the always condition "al".
     */
    private static boolean isConditionalMnemonic(Token mnemonicToken, String baseMnemonic)
    {
        if (mnemonicToken == null) return false;

        String text = mnemonicToken.getText();
        if (text == null) return false;

        text = text.toLowerCase();
        baseMnemonic = baseMnemonic.toLowerCase();

        if (!text.startsWith(baseMnemonic))
        {
            // unexpected, but then we do not flag here
            return false;
        }

        String suffix = text.substring(baseMnemonic.length()); // "" or "ne" or "al" etc.
        if (suffix.isEmpty()) return false;      // plain "b", "bl", ...
        if (suffix.equals("al")) return false;   // "bal"/"blal" treated as unconditional
        return true;
    }
}
