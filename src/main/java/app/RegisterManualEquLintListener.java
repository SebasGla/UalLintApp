package app;

import antlr4gen.*;
import org.antlr.v4.runtime.Token;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/*
 * Linting rule to ensure user-defined constants match the hardware register LUT.
 * If a developer types `.equ MY_UART, 0x4000`, this checks if 0x4000 is actually
 * registered as something else (like "UART_RX"), or if "MY_UART" is registered
 * to a completely different address. Prevents nasty hardware-level typos.
 */
public final class RegisterManualEquLintListener extends LinterParserBaseListener
{
    private static final Rules ruleId = Rules.RegisterFromManualEqu;

    private final DiagnosticCollector diags;
    private final Map<Long, String> addrToName; // Fast lookup: "What is at 0x4000?"
    private final Map<String, Long> nameToAddr; // Fast lookup: "Where is UART_RX?"

    public RegisterManualEquLintListener(DiagnosticCollector diags, Map<Long, String> addrToName)
    {
        this.diags = diags;
        this.addrToName = (addrToName != null) ? addrToName : Map.of();

        // Build the reverse lookup table right away so we can easily check Name -> Address
        this.nameToAddr = buildReverseMap(this.addrToName);
    }

    /*
     * Intercepts every `.equ NAME, VALUE` directive in the code.
     */
    @Override
    public void exitEquDirective(LinterParser.EquDirectiveContext ctx)
    {
        // Safety check to ensure the parser actually found a label name
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

        // Try to read the address. Note: This rule only checks plain hex values right now.
        Long addr = tryParsePlainHexAddress(ctx.constExpr());

        if (addr == null)
        {
            return;
        }

        // -----------------------------
        // (1) Address -> Name check
        // -----------------------------
        // The developer used a specific memory address. Does our LUT know what it is?
        String expectedNameForAddr = addrToName.get(addr);

        if (expectedNameForAddr != null)
        {
            // The address is in our LUT, but they named it something else!
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
        // The developer used a name that exists in our LUT. Did they point it to the right place?
        Long expectedAddrForName = nameToAddr.get(normalizeName(actualName));

        if (expectedAddrForName != null)
        {
            // The name is right, but the hex address is completely wrong!
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

    /*
     * Parses the constant expression, but only if it's a simple, raw hex string (e.g., 0x4000).
     * We don't try to evaluate complex math here, as that is handled by other rules.
     */
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

        // Strip out any formatting underscores (e.g., 0x4000_0000)
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

    /*
     * Flips the Address->Name map into a Name->Address map.
     * Lowercases the keys to make lookups case-insensitive.
     */
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

            // First one wins (prevents crashing if the LUT has duplicate names)
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

    /*
     * Helper to pretty-print memory addresses in the error messages.
     */
    private static String formatHex(long v)
    {
        return "0x" + Long.toHexString(v).toUpperCase(Locale.ROOT);
    }
}