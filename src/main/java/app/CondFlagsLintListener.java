package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

/**
 * Rule: CONDFLAGS
 * Only set flags (e.g., ADDS/SUBS) when they are tested/branched soon after.
 *
 * Semantics used here:
 * - Track a pending NZCV producer (flag-setting instruction).
 * - It must be consumed by an IT instruction or a conditional branch within LOOKAHEAD instructions.
 * - If flags are overwritten (CMP/TEST or another flag-setting op) before a use, report the producer.
 * - If an unconditional branch occurs while flags are pending, report the producer.
 */
public final class CondFlagsLintListener extends LinterParserBaseListener
{

    private static final int LOOKAHEAD = 3; // window until the flag has to be consumed

    private final DiagnosticCollector diags;

    // pending flag producer
    private Token pendingProducer = null;
    private int remaining = 0;                      // number of future instructions allowed until "use"

    public CondFlagsLintListener(DiagnosticCollector diags)
    {
        this.diags = diags;
    }

    @Override
    public void exitInstruction(LinterParser.InstructionContext ctx)
    {
        //If a pendingProducer is already being tracked then decide what the current instruction does to it.
        if (pendingProducer != null)
        {
            // Flags are used: IT or conditional branch.
            if (isFlagConsumer(ctx))
            {
                clearPending(); // satisfied; do not report
            }
            //Flags are overwritten: another S-flag instruction or CMP/TEST.
            else if (isFlagClobberer(ctx))
            {
                reportPending("Flags are set but overwritten before being tested/branched after.");
                clearPending();
            }
            // Unconditional branch: flags cannot be used anymore because of unconditional jump
            else if (isUnconditionalBranch(ctx))
            {
                reportPending("Flags are set but control flow leaves before they are tested/branched after.");
                clearPending();
            }
            // Otherwise: consume one step of the lookahead window.
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

        // After handling the old pending state, see if instruction sets flags.
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
        diags.report(Rules.CONDFLAGS, Severity.WARNING, pendingProducer, msg);
    }

    // -------------------------
    // Classification functions
    // -------------------------

    /**
     * Returns a token if the instruction sets NZCV flags (e.g. from ADDS/SUBS).
     * (You can later extend this to LOGIC+SFLAG, MOV+SFLAG, shifts with S, etc.)
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
            // base mnemonic length varies (umull, umlal, smull, smlal) => 5
            if (hasSFlag(t, 5))
                return t;
        }

        return null;
    }

    /**
     * - IT <cond> consumes flags
     * - conditional branches b<cond>/bl<cond>/bx<cond>/blx<cond> consume flags
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

    /**
     * Clobbers flags (overwrites NZCV) and therefore invalidates a pending producer.
     * - CMP (cmp/cmn) overwrites flags
     * - TEST (tst/teq) overwrites flags
     * - A second flag producer overwrites flags
     */

    private static boolean isFlagClobberer(LinterParser.InstructionContext ctx)
    {

        if (ctx.compInstr() != null)
        {
            return true; // CMP/CMN overwrite flags
        }
        if (ctx.testInstr() != null)
        {
            return true; // TST/TEQ overwrite flags
        }

        // Another flag producer overwrites flags as well
        return getFlagProducerToken(ctx) != null;
    }

    /**
     * Unconditional branch terminates the current linear in consume window.
     * - b label (no cond suffix) as unconditional branch
     * - similarly bl/blx/bx/bxj without cond suffix
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

    /**
     * Checks whether a token text encodes an S-flag right after the base mnemonic.
     *
     * @param mnemonicToken the token containing mnemonic(+suffixes)
     * @param baseLen length of base mnemonic (e.g., add/sub/... are length 3)
     */
    private static boolean hasSFlag(Token mnemonicToken, int baseLen)
    {
        if (mnemonicToken == null)
            return false;

        String text = mnemonicToken.getText();
        if (text == null)
            return false;

        text = text.toLowerCase();
        if (text.length() <= baseLen) return false;

        // For UAL in your lexer: opcode + optional 's' + optional cond
        return text.charAt(baseLen) == 's';
    }

    /**
     * Your lexer folds {cond} into the mnemonic token text (e.g., "bne", "blt", "bxge").
     * This checks if the mnemonic has a condition suffix (except "al").
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

        String suffix = text.substring(baseMnemonic.length()); // "" or "ne" or "al" ...
        if (suffix.isEmpty())
            return false;
        if (suffix.equals("al"))
            return false;

        return true;
    }

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
