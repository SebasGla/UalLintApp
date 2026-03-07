package app;

import org.antlr.v4.runtime.Token;
import antlr4gen.*;

/*
 * Linting rule to ensure conditional branches are not placed inside IT (If-Then) blocks.
 * In ARM assembly, putting instructions like 'BEQ' or 'CBZ' inside an IT block
 * is generally invalid or highly discouraged because it breaks the predictable control flow.
 */
public final class ItBlockLintListener extends LinterParserBaseListener
{

    private static final Rules ruleId = Rules.ItBlock;

    private final DiagnosticCollector diags;

    // Tracks how many instructions are left to process inside the current IT block
    private int itRemaining = 0;

    public ItBlockLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    // ---------------------------
    // 1) Trigger on IT instruction
    // ---------------------------

    /*
     * Hook that triggers when we hit an IT instruction.
     * We calculate the size of the block based on the mnemonic length
     * (e.g., "ITT" = 3 chars, meaning 2 instructions follow).
     */
    @Override
    public void exitItInstr(LinterParser.ItInstrContext ctx) {
        String itText = ctx.IT().getText().toLowerCase(); // Normalize to lowercase just in case

        // The length of the block is simply the string length minus 1
        // Example: "ite" (length 3) -> 2 instructions in the block
        int blockLen = Math.max(1, itText.length() - 1);

        // Reset our counter to track the upcoming instructions
        itRemaining = blockLen;
    }

    // --------------------------------------------
    // 2) For every instruction, check IT-block body
    // --------------------------------------------

    /*
     * Hook that runs for every instruction.
     * If we are currently inside an active IT block, we check the instruction for violations.
     */
    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx) {
        // Skip the actual IT instruction itself so we don't accidentally count it
        if (ctx.itInstr() != null) {
            return;
        }

        // If the counter is 0, we aren't in an IT block. Just move on.
        if (itRemaining <= 0) {
            return;
        }

        // We are officially inside the IT block body.
        // Check if the developer tried to sneak a conditional branch in here.
        Token offending = getConditionalBranchToken(ctx);
        if (offending != null)
        {
            diags.report(ruleId, Severity.ERROR, offending, "Conditional branch must not appear inside an IT block.");
        }

        // Tick down the counter since we just processed one of the IT block's instructions
        itRemaining--;
    }

    /*
     * Scans the instruction to see if it's a conditional branch.
     * Returns the offending token if it is, otherwise returns null.
     */
    private static Token getConditionalBranchToken(LinterParser.InstructionContext ctx)
    {

        // CBZ (Compare and Branch on Zero) and CBNZ are inherently conditional
        if (ctx.cbzInstr() != null)
        {
            return ctx.cbzInstr().getStart();
        }

        // Check standard branch instructions (b, bl, blx, bx, bxj)
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

    /*
     * Helper to detect if a branch mnemonic has a condition attached.
     * Extracts suffixes like 'ne' from 'bne' and verifies they aren't unconditional.
     */
    private static boolean isConditionalMnemonic(Token mnemonicToken, String baseMnemonic)
    {
        if (mnemonicToken == null) return false;

        String text = mnemonicToken.getText();
        if (text == null) return false;

        text = text.toLowerCase();
        baseMnemonic = baseMnemonic.toLowerCase();

        // Safety check to ensure the token actually starts with our expected instruction
        if (!text.startsWith(baseMnemonic))
        {
            return false;
        }

        // Grab whatever text comes after the base mnemonic (e.g., the "ne" in "bne")
        String suffix = text.substring(baseMnemonic.length());

        // Plain "b" or "bl" is unconditional
        if (suffix.isEmpty()) return false;

        // "bal" (branch always) is practically unconditional
        if (suffix.equals("al")) return false;

        return true; // It has a valid condition suffix
    }
}