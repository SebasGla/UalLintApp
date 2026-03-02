package app;

import antlr4gen.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.List;
import java.util.Map;

public final class LinterRunner
{
    private LinterRunner()
    {
    }

    // Signatur um 'List<String> enabledRules' erweitert
    public static DiagnosticCollector run(String sourceText, String sourceName, Map<Long, String> addrToName, List<String> enabledRules)
    {

        LinterLexer lexer = new LinterLexer(CharStreams.fromString(sourceText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LinterParser parser = new LinterParser(tokens);

        ParseTree tree = parser.program();
        DiagnosticCollector dc = new DiagnosticCollector(sourceName);

        // Bedingte Ausführung der Regeln
        if (enabledRules.contains("BlankEof")) {
            BlankEofRule.check(sourceText, dc);
        }

        ParseTreeWalker walker = new ParseTreeWalker();

        if (enabledRules.contains("ItBlock")) walker.walk(new ItBlockLintListener(dc), tree);
        if (enabledRules.contains("CondFlags")) walker.walk(new CondFlagsLintListener(dc), tree);
        if (enabledRules.contains("ImmediateHash")) walker.walk(new ImmediateHashLintListener(dc), tree);
        if (enabledRules.contains("AbsoluteAddress")) walker.walk(new AbsoluteAddressLintListener(dc), tree);
        if (enabledRules.contains("ShortInstructionForm")) walker.walk(new LongFormLintListener(dc), tree);
        if (enabledRules.contains("ShiftPow2")) walker.walk(new ShiftPow2LintListener(dc), tree);
        if (enabledRules.contains("ThumbFunc")) walker.walk(new ThumbFuncLintListener(dc, tokens), tree);
        if (enabledRules.contains("NoCondReturns")) walker.walk(new NoCondReturnsLintListener(dc), tree);
        if (enabledRules.contains("ParamsAAPCS")) walker.walk(new ParamsAAPCSLintListener(dc), tree);
        if (enabledRules.contains("RegisterManualEqu")) walker.walk(new RegisterManualEquLintListener(dc, addrToName), tree);
        if (enabledRules.contains("RoutineReturn")) walker.walk(new RoutineReturnLintListener(dc, tokens), tree);
        if (enabledRules.contains("LogicalOp")) walker.walk(new LogicalOpLintListener(dc), tree);
        if (enabledRules.contains("RecursiveRoutine")) walker.walk(new RecursiveRoutineLintListener(dc), tree);

        return dc;
    }
}