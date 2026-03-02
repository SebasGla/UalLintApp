package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public final class RegisterManualEquLintListener extends LinterParserBaseListener
{
    private static final Rules ruleId = Rules.RegisterFromManualEqu;

    private final DiagnosticCollector diags;
    private final Map<Long, String> addrToName;
    private final Map<String, Long> nameToAddr;

    public RegisterManualEquLintListener(DiagnosticCollector diags, Map<Long, String> addrToName)
    {
        this.diags = diags;
        this.addrToName = (addrToName != null) ? addrToName : Map.of();
        this.nameToAddr = buildReverseMap(this.addrToName);
    }

    @Override
    public void exitEquDirective(LinterParser.EquDirectiveContext ctx)
    {
        if (ctx.ID() == null)
        {
            System.out.println("BAD EQU near line " + ctx.getStart().getLine() + ": " + ctx.getText());
            return;
        }

        if (ctx.ID() == null) {
            diags.reportAt(
                    ruleId,
                    Severity.ERROR,
                    ctx.getStart().getLine(),
                    ctx.getStart().getCharPositionInLine(),
                    "Bad .equ directive: expected `.equ <NAME>, <EXPR>`.",
                    ctx.getText()
            );
            return;
        }

        Token nameTok = ctx.ID().getSymbol();
        String actualName = safeText(nameTok);

        Long addr = tryParsePlainHexAddress(ctx.constExpr());

        if (addr == null)
        {
            return;
        }

        // -----------------------------
        // (1) Address -> Name check
        // -----------------------------
        String expectedNameForAddr = addrToName.get(addr);

        if (expectedNameForAddr != null)
        {
            if (!equalsIgnoreCase(actualName, expectedNameForAddr))
            {
                diags.report(
                        ruleId,
                        Severity.WARNING,
                        nameTok,
                        "Register name mismatch: address "
                                + formatHex(addr)
                                + " is `"
                                + expectedNameForAddr
                                + "` in the LUT, but is defined as `"
                                + actualName
                                + "`."
                );
            }
        }

        // -----------------------------
        // (2) Name -> Address check
        // -----------------------------
        Long expectedAddrForName = nameToAddr.get(normalizeName(actualName));

        if (expectedAddrForName != null)
        {
            if (!expectedAddrForName.equals(addr))
            {
                diags.report(
                        ruleId,
                        Severity.ERROR,
                        nameTok,
                        "Reference manual mismatch: `"
                                + actualName
                                + "` is "
                                + formatHex(expectedAddrForName)
                                + " in the LUT, but this .equ uses "
                                + formatHex(addr)
                                + "."
                );
            }
        }
    }

    private static Long tryParsePlainHexAddress(LinterParser.ConstExprContext expr)
    {
        if (expr == null)
        {
            return null;
        }

        String t = expr.getText();

        if (t == null)
        {
            return null;
        }

        t = t.trim().replace("_", "");

        if (t.startsWith("0x") == false && t.startsWith("0X") == false)
        {
            return null;
        }

        String hex = t.substring(2);

        if (hex.isEmpty() == true)
        {
            return null;
        }

        try
        {
            return Long.parseUnsignedLong(hex, 16);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }



    private static Map<String, Long> buildReverseMap(Map<Long, String> addrToName)
    {
        Map<String, Long> m = new HashMap<>();

        for (Map.Entry<Long, String> e : addrToName.entrySet())
        {
            Long addr = e.getKey();
            String name = e.getValue();

            if (addr == null)
            {
                continue;
            }

            if (name == null)
            {
                continue;
            }

            String key = normalizeName(name);

            if (key.isEmpty())
            {
                continue;
            }

            // first one wins (avoid flapping if LUT has duplicates)
            if (!m.containsKey(key))
            {
                m.put(key, addr);
            }
        }

        return m;
    }

    private static String normalizeName(String s)
    {
        if (s == null)
        {
            return "";
        }

        return s.trim().toLowerCase(Locale.ROOT);
    }

    private static boolean equalsIgnoreCase(String a, String b)
    {
        if (a == null && b == null)
        {
            return true;
        }

        if (a == null || b == null)
        {
            return false;
        }

        return a.trim().equalsIgnoreCase(b.trim());
    }

    private static String safeText(Token t)
    {
        if (t == null)
        {
            return "";
        }

        String s = t.getText();

        if (s == null)
        {
            return "";
        }

        return s;
    }

    private static String formatHex(long v)
    {
        return "0x" + Long.toHexString(v).toUpperCase(Locale.ROOT);
    }
}
