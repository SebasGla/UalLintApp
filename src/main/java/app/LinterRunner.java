package app;

import antlr4gen.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

import java.util.List;
import java.util.Map;

/*
 * The main execution engine of the linter.
 * It glues everything together: takes the raw source text, runs it through the ANTLR
 * lexer and parser, builds the tree, and fires off only the rules the user enabled.
 */
public final class LinterRunner
{
    private LinterRunner()
    {
        // Prevent instantiation, this is purely a utility class
    }

    /*
     * Parses the code and runs all active linting checks.
     * Returns a populated collector containing every error and warning found.
     */
    public static DiagnosticCollector run(String sourceText, String sourceName, Map<Long, String> addrToName, List<String> enabledRules)
    {
        // 1. Lexing: Break the raw text string down into categorized tokens
        LinterLexer lexer = new LinterLexer(CharStreams.fromString(sourceText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        // 2. Parsing: Structure those tokens into a hierarchical parse tree
        LinterParser parser = new LinterParser(tokens);
        ParseTree tree = parser.program();

        // 3. Setup: Create the bucket where all our rules will dump their findings
        DiagnosticCollector dc = new DiagnosticCollector(sourceName);

        // Run raw text-based checks (These don't need the ANTLR tree, just the raw string)
        if (enabledRules.contains("BlankEof")) {
            BlankEofRule.check(sourceText, dc);
        }

        // The walker is what actually traverses the parse tree from top to bottom
        ParseTreeWalker walker = new ParseTreeWalker();

        // 4. Tree Walking: Conditionally attach listeners based on what the user enabled in the UI.
        // As the walker traverses the tree, these listeners will trigger on specific nodes.
        if (enabledRules.contains("ItBlock")) walker.walk(new ItBlockLintListener(dc), tree);
        if (enabledRules.contains("CondFlags")) walker.walk(new CondFlagsLintListener(dc), tree);
        if (enabledRules.contains("ImmediateHash")) walker.walk(new ImmediateHashLintListener(dc), tree);
        if (enabledRules.contains("AbsoluteAddress")) walker.walk(new AbsoluteAddressLintListener(dc), tree);
        if (enabledRules.contains("ShortInstructionForm")) walker.walk(new LongFormLintListener(dc), tree);
        if (enabledRules.contains("ShiftPow2")) walker.walk(new ShiftPow2LintListener(dc), tree);
        if (enabledRules.contains("ThumbFunc")) walker.walk(new ThumbFuncLintListener(dc, tokens), tree);
        if (enabledRules.contains("NoCondReturns")) walker.walk(new NoCondReturnsLintListener(dc), tree);
        if (enabledRules.contains("ParamsAAPCS")) walker.walk(new ParamsAAPCSLintListener(dc), tree);

        // This rule needs the custom hardware register LUT to check memory addresses
        if (enabledRules.contains("RegisterManualEqu")) walker.walk(new RegisterManualEquLintListener(dc, addrToName), tree);

        if (enabledRules.contains("RoutineReturn")) walker.walk(new RoutineReturnLintListener(dc, tokens), tree);
        if (enabledRules.contains("LogicalOp")) walker.walk(new LogicalOpLintListener(dc), tree);
        if (enabledRules.contains("RecursiveRoutine")) walker.walk(new RecursiveRoutineLintListener(dc), tree);

        return dc;
    }
}