package app;

import antlr4gen.*;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.*;

public final class LinterRunner
{
    private LinterRunner()
    {
    }

    public static DiagnosticCollector run(String sourceText, String sourceName)
    {
        LinterLexer lexer = new LinterLexer(CharStreams.fromString(sourceText));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        LinterParser parser = new LinterParser(tokens);

        ParseTree tree = parser.program();

        DiagnosticCollector dc = new DiagnosticCollector(sourceName);

        ParseTreeWalker walker = new ParseTreeWalker();
        walker.walk(new ItBlockLintListener(dc), tree);
        walker.walk(new CondFlagsLintListener(dc), tree);
        walker.walk(new ImmediateHashLintListener(dc), tree);
        walker.walk(new AbsoluteAddressLintListener(dc), tree);
        walker.walk(new LongFormLintListener(dc), tree);
        walker.walk(new ShiftPow2LintListener(dc), tree);

        return dc;
    }
}
