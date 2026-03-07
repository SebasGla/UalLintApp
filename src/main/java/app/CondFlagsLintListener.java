package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

/*
 * Linting rule to catch unnecessary condition flags.
 * If an instruction sets flags (like ADDS or SUBS), those flags must be used
 * by an IT block or a conditional branch shortly after. If they are overwritten
 * or abandoned, we warn the user.
 */
public final class CondFlagsLintListener extends LinterParserBaseListener
{

    // How many instructions we check before deciding the flags were wasted
    private static final int LOOKAHEAD = 3;

    private final DiagnosticCollector diags;

    // The instruction token that just set the flags (e.g., the ADDS instruction)
    private Token pendingProducer = null;

    // Countdown until we report the pending producer as unused
    private int remaining = 0;

    public CondFlagsLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    /*
     * Main evaluation hook. Called for every single instruction parsed.
     * Manages the lifecycle of condition flags: checking if they are used,
     * overwritten, or abandoned.
     */
    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx)
    {
        // If we are currently tracking an instruction that set the flags...
        if (pendingProducer != null)
        {
            // Case 1: The flags are properly used.
            if (isFlagConsumer(ctx))
            {
                clearPending();
            }
            // Case 2: The flags are blindly overwritten by another CMP/TEST or 'S' instruction.
            else if (isFlagClobberer(ctx))
            {
                reportPending("Flags are set but overwritten before being tested/branched after.");
                clearPending();
            }
            // Case 3: We jump away unconditionally, abandoning the flags.
            else if (isUnconditionalBranch(ctx))
            {
                reportPending("Flags are set but control flow leaves before they are tested/branched after.");
                clearPending();
            }
            // Case 4: Not used yet. Tick down the countdown timer.
            else
            {
                remaining--;
                if (remaining <= 0)
                {
                    reportPending("Flags are set but not tested/branched after.");
                    clearPending();
                }
            }
        }

        // Check if the current instruction is setting new flags for future instructions
        Token producer = getFlagProducerToken(ctx);
        if (producer != null)
        {
            pendingProducer = producer;
            remaining = LOOKAHEAD;
        }
    }

    // -------------------------
    // Pending management
    // -------------------------

    private void clearPending()
    {
        pendingProducer = null;
        remaining = 0;
    }

    private void reportPending(String msg)
    {
        diags.report(Rules.CondFlags, Severity.WARNING, pendingProducer, msg);
    }

    // -------------------------
    // Classification functions
    // -------------------------

    /*
     * Scans the instruction tree to see if it modifies the condition flags.
     * Looks for the 'S' suffix on arithmetic, logical, and shift operations.
     */
    private static Token getFlagProducerToken(LinterParser.InstructionContext ctx)
    {
        if (ctx.arithmeticInstr() != null)
        {
            Token t = ctx.arithmeticInstr().getStart();
            if (hasSFlag(t, 3))
                return t;
        }

        if (ctx.movInstr() != null)
        {
            Token t = ctx.movInstr().MOV().getSymbol();
            if (hasSFlag(t, 3))
                return t;
        }

        if (ctx.logicInstr() != null)
        {
            Token t = ctx.logicInstr().getStart();
            if (hasSFlag(t, 3))
                return t;
        }

        if (ctx.mulInstr() != null)
        {
            Token t = ctx.mulInstr().getStart();
            if (hasSFlag(t, 3))
                return t;
        }

        if (ctx.mulASInstr() != null)
        {
            if (ctx.mulASInstr().MLA() != null)
            {
                Token t = ctx.mulASInstr().MLA().getSymbol();
                if (hasSFlag(t, 3))
                    return t;
            }
        }

        if (ctx.shiftInstruction() != null)
        {
            Token t = ctx.shiftInstruction().getStart();
            if (hasSFlag(t, 3))
                return t;
        }

        if (ctx.rrxIntrstuction() != null)
        {
            Token t = ctx.rrxIntrstuction().RRX().getSymbol();
            if (hasSFlag(t, 3))
                return t;
        }

        if (ctx.negInstr() != null)
        {
            Token t = ctx.negInstr().NEG().getSymbol();
            if (hasSFlag(t, 3))
                return t;
        }

        if (ctx.longMultiplyInstr() != null)
        {
            Token t = ctx.longMultiplyInstr().LONGMULTIPLY().getSymbol();
            // Base mnemonic length varies for these (umull, umlal, smull, smlal) so we pass 5
            if (hasSFlag(t, 5))
                return t;
        }

        return null;
    }

    /*
     * Determines if an instruction actually reads/consumes the condition flags.
     * IT blocks and conditional branches are the primary consumers.
     */
    private static boolean isFlagConsumer(LinterParser.InstructionContext ctx)
    {

        if (ctx.itInstr() != null)
        {
            return true;
        }

        if (ctx.branch() != null)
        {
            LinterParser.BranchContext b = ctx.branch();

            if (b.BRANCH() != null)
            {
                return isConditionalMnemonic(b.BRANCH().getSymbol(), "b");
            }

            if (b.BRANCHLINK() != null)
            {
                return isConditionalMnemonic(b.BRANCHLINK().getSymbol(), "bl");
            }

            if (b.BRANCHLINKX() != null)
            {
                return isConditionalMnemonic(b.BRANCHLINKX().getSymbol(), "blx");
            }

            if (b.BRANCHX() != null)
            {
                return isConditionalMnemonic(b.BRANCHX().getSymbol(), "bx");
            }

            if (b.BRANXJAZELLE() != null)
            {
                return isConditionalMnemonic(b.BRANXJAZELLE().getSymbol(), "bxj");
            }
        }

        return false;
    }

    /*
     * Checks if an instruction overwrites the current flags without using them first.
     * CMP and TST naturally overwrite flags, as do any other 'S' suffixed instructions.
     */
    private static boolean isFlagClobberer(LinterParser.InstructionContext ctx)
    {

        if (ctx.compInstr() != null)
        {
            return true; // CMP/CMN overwrite flags immediately
        }
        if (ctx.testInstr() != null)
        {
            return true; // TST/TEQ overwrite flags immediately
        }

        // If it's a completely different instruction that also sets the flags, it clobbers the old ones
        return getFlagProducerToken(ctx) != null;
    }

    /*
     * Identifies branches that jump no matter what the condition flags say.
     * If we hit one of these while holding pending flags, those flags are lost.
     */
    private static boolean isUnconditionalBranch(LinterParser.InstructionContext ctx)
    {
        if (ctx.branch() == null) return false;

        LinterParser.BranchContext b = ctx.branch();

        if (b.BRANCH() != null)
        {
            return isUnconditionalMnemonic(b.BRANCH().getSymbol(), "b");
        }
        if (b.BRANCHLINK() != null)
        {
            return isUnconditionalMnemonic(b.BRANCHLINK().getSymbol(), "bl");
        }
        if (b.BRANCHLINKX() != null)
        {
            return isUnconditionalMnemonic(b.BRANCHLINKX().getSymbol(), "blx");
        }
        if (b.BRANCHX() != null)
        {
            return isUnconditionalMnemonic(b.BRANCHX().getSymbol(), "bx");
        }
        if (b.BRANXJAZELLE() != null)
        {
            return isUnconditionalMnemonic(b.BRANXJAZELLE().getSymbol(), "bxj");
        }

        return false;
    }

    // -------------------------
    // Mnemonic text helpers
    // -------------------------

    /*
     * Helper to parse the raw text of a mnemonic and find the 'S' flag.
     * We pass in the base length (e.g., 3 for "add") to check the exact character position.
     */
    private static boolean hasSFlag(Token mnemonicToken, int baseLen)
    {
        if (mnemonicToken == null)
            return false;

        String text = mnemonicToken.getText();
        if (text == null)
            return false;

        text = text.toLowerCase(); // Standardize to lowercase for easy checking
        if (text.length() <= baseLen) return false;

        // Our lexer groups the opcode, optional 's', and optional conditions together
        return text.charAt(baseLen) == 's';
    }

    /*
     * Extracts the condition suffix from a branch instruction (e.g., the 'ne' in 'bne').
     * Explicitly ignores the 'al' (always) suffix since it acts like an unconditional branch.
     */
    private static boolean isConditionalMnemonic(Token mnemonicToken, String baseMnemonic)
    {
        if (mnemonicToken == null)
            return false;

        String text = mnemonicToken.getText();
        if (text == null)
            return false;

        text = text.toLowerCase();
        baseMnemonic = baseMnemonic.toLowerCase();

        if (!text.startsWith(baseMnemonic))
            return false;

        String suffix = text.substring(baseMnemonic.length());

        // No suffix or "al" means it's unconditional
        if (suffix.isEmpty())
            return false;
        if (suffix.equals("al"))
            return false;

        return true;
    }

    /*
     * Checks if a branch is unconditional, meaning it has no suffix or explicitly uses 'al'.
     */
    private static boolean isUnconditionalMnemonic(Token mnemonicToken, String baseMnemonic)
    {
        if (mnemonicToken == null)
            return false;
        String text = mnemonicToken.getText();
        if (text == null)
            return false;

        text = text.toLowerCase();
        baseMnemonic = baseMnemonic.toLowerCase();

        if (!text.startsWith(baseMnemonic)) return false;

        String suffix = text.substring(baseMnemonic.length());
        return suffix.isEmpty() || suffix.equals("al");
    }
}