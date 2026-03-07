package app;

import antlr4gen.LinterLexer;
import antlr4gen.LinterParser;
import antlr4gen.LinterParserBaseListener;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/*
 * Linting rule to verify that functions return their values in the correct registers.
 * According to AAPCS, single values go in r0 and 64-bit values go in r1:r0.
 * This listener looks at the last instructions before a return to see what was actually written.
 */
public final class RoutineReturnLintListener extends LinterParserBaseListener
{
    private static final Rules ruleId = Rules.RoutineReturn;

    private final DiagnosticCollector diags;
    private final List<Token> tokens; // Full stream of tokens for deep inspection

    public RoutineReturnLintListener(
            DiagnosticCollector diags,
            CommonTokenStream tokenStream
    )
    {
        this.diags = diags;
        this.tokens = tokenStream.getTokens();
    }

    /*
     * Triggered when exiting a routine. This is the main logic for checking
     * return value consistency before the function ends.
     */
    @Override
    public void exitRoutine(LinterParser.RoutineContext ctx)
    {
        LinterParser.RoutineBodyContext body;
        LinterParser.RoutineBlockContext block;
        ReturnSite ret; // Where the function actually exits (BX LR or POP {PC})
        List<LinterParser.InstructionContext> insns;
        LinterParser.InstructionContext lastBeforeReturn;
        Set<String> poppedRegs; // Registers that were just being restored from the stack
        LastTwoWriters lastTwo; // The last two instructions that actually modified registers
        Set<String> union;

        body = ctx.routineBody();
        ret = getReturnSite(body);

        // If we can't find how the function returns, we can't check the values
        if (ret == null)
        {
            return;
        }

        block = getRoutineBlock(body);

        if (block == null)
        {
            return;
        }

        insns = new ArrayList<>();

        // Grab every instruction inside the function body
        collectInstructions(
                block,
                insns
        );

        // Ensure instructions are in the order they appear in the file
        insns.sort(
                Comparator.comparingInt(
                        i -> i.getStart().getTokenIndex()
                )
        );

        lastBeforeReturn = lastInstructionBefore(
                insns,
                ret.returnTokenIndex
        );

        // If the very last thing is a branch, it's likely a tail-call optimization, so skip check
        if (
                lastBeforeReturn != null
                        && isBranchLikeByText(lastBeforeReturn)
        )
        {
            return;
        }

        poppedRegs = new HashSet<>();

        // If the return happens via a POP, those registers aren't "return values", they are state restoration
        if (ret.popReturn != null)
        {
            poppedRegs.addAll(
                    expandRegList(
                            ret.popReturn.regList()
                    )
            );
        }

        // Collect all registers ever popped in the function to filter out "restore" operations
        for (LinterParser.InstructionContext ins : insns)
        {
            if (ins.pop() == null)
            {
                continue;
            }

            poppedRegs.addAll(
                    expandRegList(
                            ins.pop().regList()
                    )
            );
        }

        poppedRegs.remove("pc");
        poppedRegs.remove("r15");

        // Find the "real" instructions that set the final result
        lastTwo = findLastTwoCandidateWriters(
                insns,
                ret.returnTokenIndex,
                poppedRegs
        );

        if (lastTwo == null)
        {
            return;
        }

        union = new LinkedHashSet<>();
        union.addAll(lastTwo.firstWritten);
        union.addAll(lastTwo.secondWritten);

        if (union.isEmpty())
        {
            return;
        }

        // If they wrote to r0 (or r0 and r1), it's a valid AAPCS return
        if (isAcceptedReturnSet(union))
        {
            return;
        }

        diags.report(
                ruleId,
                Severity.ERROR,
                lastTwo.firstAt != null ? lastTwo.firstAt : ctx.getStart(),
                "Return value must be in r0 (or r1:r0 for 64-bit). "
                        + "Last two candidate writer instructions wrote: "
                        + joinRegs(union)
                        + "."
        );
    }

    /*
     * Walks backward from the return point to find the last two instructions
     * that modified registers that weren't immediately consumed or just restored from stack.
     */
    private LastTwoWriters findLastTwoCandidateWriters(
            List<LinterParser.InstructionContext> insns,
            int returnTokenIndex,
            Set<String> poppedRegs
    )
    {
        Token firstAt;
        Token secondAt;
        Set<String> firstWritten;
        Set<String> secondWritten;

        firstAt = null;
        secondAt = null;

        firstWritten = new LinkedHashSet<>();
        secondWritten = new LinkedHashSet<>();

        // Iterate backwards from the return point
        for (int i = insns.size() - 1; i >= 0; i--)
        {
            LinterParser.InstructionContext ins;
            int idx;
            Set<String> rawWrites;
            Set<String> candWrites;

            ins = insns.get(i);
            idx = ins.getStart().getTokenIndex();

            if (idx >= returnTokenIndex)
            {
                continue;
            }

            rawWrites = writtenRegsRaw(ins);

            if (rawWrites.isEmpty())
            {
                continue;
            }

            candWrites = new LinkedHashSet<>(rawWrites);

            // Ignore registers that were just being restored to satisfy the caller (callee-saved)
            candWrites.removeAll(poppedRegs);

            if (candWrites.isEmpty())
            {
                continue;
            }

            // Ignore registers that were just used as temporary scratchpad values earlier in the block
            candWrites.removeIf(
                    r -> isConsumedLater(
                            r,
                            idx,
                            insns,
                            returnTokenIndex
                    )
            );

            if (candWrites.isEmpty())
            {
                continue;
            }

            // Track the first (closest to return) writer found
            if (firstAt == null)
            {
                firstAt = ins.getStart();
                firstWritten.addAll(candWrites);
                continue;
            }

            // Track the second writer found (for 64-bit checks)
            secondAt = ins.getStart();
            secondWritten.addAll(candWrites);
            break;
        }

        if (firstAt == null)
        {
            return null;
        }

        return new LastTwoWriters(
                firstAt,
                secondAt,
                firstWritten,
                secondWritten
        );
    }

    /*
     * Checks if a register write was just a temporary value used by a following instruction.
     */
    private boolean isConsumedLater(
            String reg,
            int writerTokenIndex,
            List<LinterParser.InstructionContext> insns,
            int returnTokenIndex
    )
    {
        for (LinterParser.InstructionContext ins : insns)
        {
            int idx;
            Set<String> reads;

            idx = ins.getStart().getTokenIndex();

            // Only look at instructions happening after the write but before the return
            if (idx <= writerTokenIndex)
            {
                continue;
            }

            if (idx >= returnTokenIndex)
            {
                break;
            }

            reads = readRegs(ins);

            if (reads.contains(reg))
            {
                return true;
            }
        }

        return false;
    }

    /*
     * Identifies all registers that an instruction reads from.
     * Includes special handling for function calls (BL) which implicitly read r0-r3.
     */
    private Set<String> readRegs(LinterParser.InstructionContext ins)
    {
        LinkedHashSet<String> out;

        out = new LinkedHashSet<>();

        if (ins == null)
        {
            return out;
        }

        // Branch with Link (calls) assume the first 4 registers are being passed as args
        if (isCallInstruction(ins))
        {
            out.add("r0");
            out.add("r1");
            out.add("r2");
            out.add("r3");

            out.addAll(
                    allRegisterTokens(
                            ins.getStart().getTokenIndex(),
                            ins.getStop().getTokenIndex()
                    )
            );

            return out;
        }

        // LDR R0, =LABEL doesn't read any registers, it just loads a constant
        if (
                ins.ldrInstr() != null
                        && isLdrLiteral(ins.getStart(), ins.getStop())
        )
        {
            return out;
        }

        List<String> all;

        all = allRegisterTokens(
                ins.getStart().getTokenIndex(),
                ins.getStop().getTokenIndex()
        );

        // If the instruction doesn't write anything (like CMP), then every register mentioned is a read
        if (isNonWriterInstruction(ins))
        {
            out.addAll(all);

            return out;
        }

        int destCount;

        // In standard instructions, the first register(s) are destinations, the rest are reads
        destCount = destRegisterCount(ins);

        for (int i = 0; i < all.size(); i++)
        {
            if (i < destCount)
            {
                continue;
            }

            out.add(all.get(i));
        }

        return out;
    }

    /*
     * Returns true if the instruction is a subroutine call (BL or BLX).
     */
    private boolean isCallInstruction(LinterParser.InstructionContext ins)
    {
        if (ins == null)
        {
            return false;
        }

        if (ins.branch() == null)
        {
            return false;
        }

        int t;

        t = ins.branch().getStart().getType();

        if (t == LinterLexer.BRANCHLINK)
        {
            return true;
        }

        if (t == LinterLexer.BRANCHLINKX)
        {
            return true;
        }

        return false;
    }

    /*
     * Finds which registers are modified by an instruction.
     * Skips control registers like SP, LR, and PC.
     */
    private Set<String> writtenRegsRaw(LinterParser.InstructionContext ins)
    {
        Set<String> out;
        int destCount;
        List<String> regs;
        String m;

        out = new LinkedHashSet<>();

        if (ins == null)
        {
            return out;
        }

        // Skip instructions that don't modify general purpose registers
        if (isNonWriterInstruction(ins))
        {
            return out;
        }

        if (
                ins.ldrInstr() != null
                        && isLdrLiteral(ins.getStart(), ins.getStop())
        )
        {
            return out;
        }

        m = ins.getStart().getText().toLowerCase();

        // Instructions that only update condition flags (ending in 's') are skipped here
        // if they don't also write to a destination register.
        if (isSFlagMnemonic(m))
        {
            return out;
        }

        destCount = destRegisterCount(ins);

        regs = firstRegisterTokens(
                ins.getStart().getTokenIndex(),
                ins.getStop().getTokenIndex(),
                destCount
        );

        for (String r : regs)
        {
            if (r.equals("sp") || r.equals("lr") || r.equals("pc"))
            {
                continue;
            }

            out.add(r);
        }

        return out;
    }

    /*
     * Checks if a mnemonic is likely setting condition flags.
     */
    private static boolean isSFlagMnemonic(String m)
    {
        if (m == null)
        {
            return false;
        }

        String t;

        t = m.toLowerCase();

        if (t.startsWith("bl") || t.startsWith("b"))
        {
            return false;
        }

        return t.endsWith("s");
    }

    /*
     * Detects if an LDR instruction is using the '=' literal syntax.
     */
    private boolean isLdrLiteral(Token start, Token stop)
    {
        int a;
        int b;

        a = start.getTokenIndex();
        b = stop.getTokenIndex();

        if (a < 0) a = 0;
        if (b >= tokens.size()) b = tokens.size() - 1;

        // Scan tokens for the '=' symbol (ASSIGN)
        for (int i = a; i <= b; i++)
        {
            if (tokens.get(i).getType() == LinterLexer.ASSIGN)
            {
                return true;
            }
        }

        return false;
    }

    /*
     * Validation check: AAPCS says returns must be in r0 (32-bit) or r1:r0 (64-bit).
     */
    private static boolean isAcceptedReturnSet(Set<String> regs)
    {
        if (regs.size() == 1)
        {
            return regs.contains("r0");
        }

        if (regs.size() == 2)
        {
            return regs.contains("r0") && regs.contains("r1");
        }

        return false;
    }

    /*
     * Returns how many destination registers an instruction writes to.
     * Most write 1, but instructions like LDRD or long multiplies write 2.
     */
    private static int destRegisterCount(LinterParser.InstructionContext ins)
    {
        if (ins.ldrdInstr() != null || ins.longMultiplyInstr() != null ||
                ins.smlalxyInstr() != null || ins.dual16bitmulInstr() != null ||
                ins.umaalInstr() != null)
        {
            return 2;
        }

        return 1;
    }

    /*
     * List of instructions that definitely don't write to general purpose registers.
     */
    private static boolean isNonWriterInstruction(LinterParser.InstructionContext ins)
    {
        return
                ins.branch() != null
                        || ins.cbzInstr() != null
                        || ins.itInstr() != null
                        || ins.compInstr() != null
                        || ins.testInstr() != null
                        || ins.nopInstr() != null
                        || ins.bkptInstr() != null
                        || ins.cpsInstr() != null
                        || ins.push() != null
                        || ins.pop() != null
                        || ins.strInstr() != null
                        || ins.strdInstr() != null
                        || ins.strtInstr() != null
                        || ins.plInstr() != null
                        || ins.dmbInstr() != null
                        || ins.clearExInstr() != null
                        || ins.debugInstr() != null
                        || ins.rfeInstr() != null
                        || ins.setendInstr() != null
                        || ins.smcInstr() != null
                        || ins.svcInstr() != null
                        || ins.eventInstr() != null
                        || ins.tbbInstr() != null
                        || ins.tbhInstr() != null
                        || ins.ldrStrMultipleInstr() != null;
    }

    /*
     * Checks if an instruction is a simple branch (B) by looking at its text.
     */
    private static boolean isBranchLikeByText(LinterParser.InstructionContext ins)
    {
        if (ins == null) return false;

        String m;

        m = ins.getStart().getText().toLowerCase();

        if (m.equals("b")) return true;

        // Match bne, beq, etc. but skip bl or bx
        if (m.startsWith("b") && !m.startsWith("bl") && !m.equals("bx"))
        {
            return true;
        }

        return false;
    }

    /*
     * Scans a token range and returns the names of the first N registers found.
     */
    private List<String> firstRegisterTokens(int from, int to, int maxCount)
    {
        List<String> out;

        out = new ArrayList<>();

        if (maxCount <= 0) return out;

        if (from < 0) from = 0;
        if (to >= tokens.size()) to = tokens.size() - 1;

        for (int i = from; i <= to; i++)
        {
            Token t;
            int type;

            t = tokens.get(i);
            type = t.getType();

            if (
                    type == LinterLexer.REGISTER
                            || type == LinterLexer.LINKREGISTER
                            || type == LinterLexer.PROGRAMCOUNTER
            )
            {
                out.add(normRegText(t.getText()));

                if (out.size() >= maxCount) break;
            }
        }

        return out;
    }

    /*
     * Scans a range and returns every register name it finds.
     */
    private List<String> allRegisterTokens(int from, int to)
    {
        List<String> out;

        out = new ArrayList<>();

        if (from < 0) from = 0;
        if (to >= tokens.size()) to = tokens.size() - 1;

        for (int i = from; i <= to; i++)
        {
            Token t;
            int type;

            t = tokens.get(i);
            type = t.getType();

            if (
                    type == LinterLexer.REGISTER
                            || type == LinterLexer.LINKREGISTER
                            || type == LinterLexer.PROGRAMCOUNTER
            )
            {
                out.add(normRegText(t.getText()));
            }
        }

        return out;
    }

    /*
     * Normalizes register names (e.g. converting r13 to sp).
     */
    private static String normRegText(String raw)
    {
        if (raw == null) return "";

        String t = raw.toLowerCase();

        if (t.equals("r13")) return "sp";
        if (t.equals("r14")) return "lr";
        if (t.equals("r15")) return "pc";

        return t;
    }

    /*
     * Converts a register list (like {r0-r3}) into a flat set of register names.
     */
    private static Set<String> expandRegList(LinterParser.RegListContext regList)
    {
        LinkedHashSet<String> out;

        out = new LinkedHashSet<>();

        if (regList == null) return out;

        for (LinterParser.RegElemContext e : regList.regElem())
        {
            if (e == null || e.register() == null) continue;

            String a = normRegText(e.register(0).getText());

            // If there's no minus sign, it's a single register
            if (e.MINUS() == null)
            {
                out.add(a);
                continue;
            }

            if (e.register().size() < 2)
            {
                out.add(a);
                continue;
            }

            String b = normRegText(e.register(1).getText());

            expandRange(out, a, b);
        }

        return out;
    }

    /*
     * Handles register ranges like r4-r7.
     */
    private static void expandRange(Set<String> out, String a, String b)
    {
        int ia;
        int ib;

        ia = regIndex(a);
        ib = regIndex(b);

        if (ia < 0 || ib < 0)
        {
            out.add(a);
            out.add(b);
            return;
        }

        // Add every register between the start and end of the range
        if (ia <= ib)
        {
            for (int i = ia; i <= ib; i++)
            {
                out.add("r" + i);
            }
            return;
        }

        for (int i = ia; i >= ib; i--)
        {
            out.add("r" + i);
        }
    }

    /*
     * Converts a register name (r0, sp, etc.) to its numerical index.
     */
    private static int regIndex(String r)
    {
        if (r == null) return -1;

        if (r.equals("sp")) return 13;
        if (r.equals("lr")) return 14;
        if (r.equals("pc")) return 15;

        if (!r.startsWith("r")) return -1;

        try
        {
            return Integer.parseInt(r.substring(1));
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    /*
     * Recursively gathers every instruction node within a parse tree block.
     */
    private static void collectInstructions(ParseTree node, List<LinterParser.InstructionContext> out)
    {
        if (node == null || node instanceof LinterParser.RoutineContext)
        {
            return;
        }

        if (node instanceof LinterParser.InstructionContext)
        {
            out.add((LinterParser.InstructionContext) node);
        }

        int n = node.getChildCount();

        for (int i = 0; i < n; i++)
        {
            collectInstructions(node.getChild(i), out);
        }
    }

    /*
     * Finds the instruction that sits immediately before a specific token index.
     */
    private static LinterParser.InstructionContext lastInstructionBefore(
            List<LinterParser.InstructionContext> insns,
            int tokenIndex
    )
    {
        LinterParser.InstructionContext last;

        last = null;

        for (LinterParser.InstructionContext i : insns)
        {
            int idx = i.getStart().getTokenIndex();

            if (idx >= tokenIndex) break;

            last = i;
        }

        return last;
    }

    /*
     * Figures out exactly how the current routine exits (POP or BX).
     */
    private static ReturnSite getReturnSite(LinterParser.RoutineBodyContext body)
    {
        Token t;

        if (body instanceof LinterParser.RoutineBxOnlyContext)
        {
            t = ((LinterParser.RoutineBxOnlyContext) body).routineBX().getStart();

            return new ReturnSite(t, t.getTokenIndex(), null);
        }

        if (body instanceof LinterParser.RoutinePopThenBxContext)
        {
            t = ((LinterParser.RoutinePopThenBxContext) body).routineBX().getStart();

            return new ReturnSite(t, t.getTokenIndex(), ((LinterParser.RoutinePopThenBxContext) body).pop());
        }

        if (body instanceof LinterParser.RoutinePopOnlyContext)
        {
            LinterParser.PopContext pop;

            pop = ((LinterParser.RoutinePopOnlyContext) body).pop();
            t = pop.getStart();

            return new ReturnSite(t, t.getTokenIndex(), pop);
        }

        return null;
    }

    /*
     * Extracts the block of code inside the function, regardless of its exit type.
     */
    private static LinterParser.RoutineBlockContext getRoutineBlock(LinterParser.RoutineBodyContext body)
    {
        if (body instanceof LinterParser.RoutineBxOnlyContext)
        {
            return ((LinterParser.RoutineBxOnlyContext) body).routineBlock();
        }

        if (body instanceof LinterParser.RoutinePopThenBxContext)
        {
            return ((LinterParser.RoutinePopThenBxContext) body).routineBlock();
        }

        if (body instanceof LinterParser.RoutinePopOnlyContext)
        {
            return ((LinterParser.RoutinePopOnlyContext) body).routineBlock();
        }

        return null;
    }

    /*
     * Simple utility to join register names into a comma-separated string for error messages.
     */
    private static String joinRegs(Set<String> regs)
    {
        StringBuilder sb;
        boolean first;

        sb = new StringBuilder();
        first = true;

        for (String r : regs)
        {
            if (!first) sb.append(", ");
            sb.append(r);
            first = false;
        }

        return sb.toString();
    }

    /*
     * Data class representing where a function returns to its caller.
     */
    private static final class ReturnSite
    {
        private final Token returnToken;
        private final int returnTokenIndex;
        private final LinterParser.PopContext popReturn;

        private ReturnSite(
                Token returnToken,
                int returnTokenIndex,
                LinterParser.PopContext popReturn
        )
        {
            this.returnToken = returnToken;
            this.returnTokenIndex = returnTokenIndex;
            this.popReturn = popReturn;
        }
    }

    /*
     * Tracks the last two instructions that modified registers.
     */
    private static final class LastTwoWriters
    {
        private final Token firstAt;
        private final Token secondAt;
        private final Set<String> firstWritten;
        private final Set<String> secondWritten;

        private LastTwoWriters(
                Token firstAt,
                Token secondAt,
                Set<String> firstWritten,
                Set<String> secondWritten
        )
        {
            this.firstAt = firstAt;
            this.secondAt = secondAt;
            this.firstWritten = firstWritten;
            this.secondWritten = secondWritten;
        }
    }
}