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

public final class RoutineReturnLintListener extends LinterParserBaseListener
{
    private static final Rules ruleId = Rules.ROUTINERETURN;

    private final DiagnosticCollector diags;
    private final List<Token> tokens;

    public RoutineReturnLintListener(
            DiagnosticCollector diags,
            CommonTokenStream tokenStream
    )
    {
        this.diags = diags;
        this.tokens = tokenStream.getTokens();
    }

    @Override
    public void exitRoutine(LinterParser.RoutineContext ctx)
    {
        LinterParser.RoutineBodyContext body;
        LinterParser.RoutineBlockContext block;
        ReturnSite ret;
        List<LinterParser.InstructionContext> insns;
        LinterParser.InstructionContext lastBeforeReturn;
        Set<String> poppedRegs;
        LastTwoWriters lastTwo;
        Set<String> union;

        body = ctx.routineBody();
        ret = getReturnSite(body);

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

        collectInstructions(
                block,
                insns
        );

        insns.sort(
                Comparator.comparingInt(
                        i -> i.getStart().getTokenIndex()
                )
        );

        lastBeforeReturn = lastInstructionBefore(
                insns,
                ret.returnTokenIndex
        );

        if (
                lastBeforeReturn != null
                        && isBranchLikeByText(lastBeforeReturn)
        )
        {
            return;
        }

        poppedRegs = new HashSet<>();

        if (ret.popReturn != null)
        {
            poppedRegs.addAll(
                    expandRegList(
                            ret.popReturn.regList()
                    )
            );
        }

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

            candWrites.removeAll(poppedRegs);

            if (candWrites.isEmpty())
            {
                continue;
            }

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

            if (firstAt == null)
            {
                firstAt = ins.getStart();
                firstWritten.addAll(candWrites);
                continue;
            }

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

    private Set<String> readRegs(LinterParser.InstructionContext ins)
    {
        LinkedHashSet<String> out;

        out = new LinkedHashSet<>();

        if (ins == null)
        {
            return out;
        }

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

        if (isNonWriterInstruction(ins))
        {
            out.addAll(all);

            return out;
        }

        int destCount;

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
            if (r.equals("sp"))
            {
                continue;
            }

            if (r.equals("lr"))
            {
                continue;
            }

            if (r.equals("pc"))
            {
                continue;
            }

            out.add(r);
        }

        return out;
    }

    private static boolean isSFlagMnemonic(String m)
    {
        if (m == null)
        {
            return false;
        }

        String t;

        t = m.toLowerCase();

        if (t.startsWith("bl"))
        {
            return false;
        }

        if (t.startsWith("b"))
        {
            return false;
        }

        return t.endsWith("s");
    }

    private boolean isLdrLiteral(Token start, Token stop)
    {
        int a;
        int b;

        a = start.getTokenIndex();
        b = stop.getTokenIndex();

        if (a < 0)
        {
            a = 0;
        }

        if (b >= tokens.size())
        {
            b = tokens.size() - 1;
        }

        for (int i = a; i <= b; i++)
        {
            if (tokens.get(i).getType() == LinterLexer.ASSIGN)
            {
                return true;
            }
        }

        return false;
    }

    private static boolean isAcceptedReturnSet(Set<String> regs)
    {
        if (regs.size() == 1)
        {
            return regs.contains("r0");
        }

        if (regs.size() == 2)
        {
            return regs.contains("r0")
                    && regs.contains("r1");
        }

        return false;
    }

    private static int destRegisterCount(LinterParser.InstructionContext ins)
    {
        if (ins.ldrdInstr() != null)
        {
            return 2;
        }

        if (ins.longMultiplyInstr() != null)
        {
            return 2;
        }

        if (ins.smlalxyInstr() != null)
        {
            return 2;
        }

        if (ins.dual16bitmulInstr() != null)
        {
            return 2;
        }

        if (ins.umaalInstr() != null)
        {
            return 2;
        }

        return 1;
    }

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

    private static boolean isBranchLikeByText(LinterParser.InstructionContext ins)
    {
        if (ins == null)
        {
            return false;
        }

        String m;

        m = ins.getStart().getText().toLowerCase();

        if (m.equals("b"))
        {
            return true;
        }

        if (
                m.startsWith("b")
                        && !m.startsWith("bl")
                        && !m.equals("bx")
        )
        {
            return true;
        }

        return false;
    }

    private List<String> firstRegisterTokens(int from, int to, int maxCount)
    {
        List<String> out;

        out = new ArrayList<>();

        if (maxCount <= 0)
        {
            return out;
        }

        if (from < 0)
        {
            from = 0;
        }

        if (to >= tokens.size())
        {
            to = tokens.size() - 1;
        }

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
                out.add(
                        normRegText(
                                t.getText()
                        )
                );

                if (out.size() >= maxCount)
                {
                    break;
                }
            }
        }

        return out;
    }

    private List<String> allRegisterTokens(int from, int to)
    {
        List<String> out;

        out = new ArrayList<>();

        if (from < 0)
        {
            from = 0;
        }

        if (to >= tokens.size())
        {
            to = tokens.size() - 1;
        }

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
                out.add(
                        normRegText(
                                t.getText()
                        )
                );
            }
        }

        return out;
    }

    private static String normRegText(String raw)
    {
        if (raw == null)
        {
            return "";
        }

        String t;

        t = raw.toLowerCase();

        if (t.equals("r13"))
        {
            return "sp";
        }

        if (t.equals("r14"))
        {
            return "lr";
        }

        if (t.equals("r15"))
        {
            return "pc";
        }

        return t;
    }

    private static Set<String> expandRegList(LinterParser.RegListContext regList)
    {
        LinkedHashSet<String> out;

        out = new LinkedHashSet<>();

        if (regList == null)
        {
            return out;
        }

        for (LinterParser.RegElemContext e : regList.regElem())
        {
            if (e == null)
            {
                continue;
            }

            if (e.register() == null)
            {
                continue;
            }

            String a;

            a = normRegText(
                    e.register(0).getText()
            );

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

            String b;

            b = normRegText(
                    e.register(1).getText()
            );

            expandRange(out, a, b);
        }

        return out;
    }

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

    private static int regIndex(String r)
    {
        if (r == null)
        {
            return -1;
        }

        if (r.equals("sp"))
        {
            return 13;
        }

        if (r.equals("lr"))
        {
            return 14;
        }

        if (r.equals("pc"))
        {
            return 15;
        }

        if (!r.startsWith("r"))
        {
            return -1;
        }

        try
        {
            return Integer.parseInt(r.substring(1));
        }
        catch (NumberFormatException ex)
        {
            return -1;
        }
    }

    private static void collectInstructions(ParseTree node, List<LinterParser.InstructionContext> out)
    {
        if (node == null)
        {
            return;
        }

        if (node instanceof LinterParser.RoutineContext)
        {
            return;
        }

        if (node instanceof LinterParser.InstructionContext)
        {
            out.add(
                    (LinterParser.InstructionContext) node
            );
        }

        int n;

        n = node.getChildCount();

        for (int i = 0; i < n; i++)
        {
            collectInstructions(
                    node.getChild(i),
                    out
            );
        }
    }

    private static LinterParser.InstructionContext lastInstructionBefore(
            List<LinterParser.InstructionContext> insns,
            int tokenIndex
    )
    {
        LinterParser.InstructionContext last;

        last = null;

        for (LinterParser.InstructionContext i : insns)
        {
            int idx;

            idx = i.getStart().getTokenIndex();

            if (idx >= tokenIndex)
            {
                break;
            }

            last = i;
        }

        return last;
    }

    private static ReturnSite getReturnSite(LinterParser.RoutineBodyContext body)
    {
        Token t;

        if (body instanceof LinterParser.RoutineBxOnlyContext)
        {
            t = ((LinterParser.RoutineBxOnlyContext) body).routineBX().getStart();

            return new ReturnSite(
                    t,
                    t.getTokenIndex(),
                    null
            );
        }

        if (body instanceof LinterParser.RoutinePopThenBxContext)
        {
            t = ((LinterParser.RoutinePopThenBxContext) body).routineBX().getStart();

            return new ReturnSite(
                    t,
                    t.getTokenIndex(),
                    ((LinterParser.RoutinePopThenBxContext) body).pop()
            );
        }

        if (body instanceof LinterParser.RoutinePopOnlyContext)
        {
            LinterParser.PopContext pop;

            pop = ((LinterParser.RoutinePopOnlyContext) body).pop();

            t = pop.getStart();

            return new ReturnSite(
                    t,
                    t.getTokenIndex(),
                    pop
            );
        }

        return null;
    }

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

    private static String joinRegs(Set<String> regs)
    {
        StringBuilder sb;
        boolean first;

        sb = new StringBuilder();
        first = true;

        for (String r : regs)
        {
            if (!first)
            {
                sb.append(", ");
            }

            sb.append(r);
            first = false;
        }

        return sb.toString();
    }

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
