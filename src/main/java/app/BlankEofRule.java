package app;

public final class BlankEofRule
{
    private BlankEofRule()
    {
    }

    public static void check(String sourceText, DiagnosticCollector dc)
    {
        if (sourceText == null)
        {
            return;
        }

        if (sourceText.isEmpty() == true)
        {
            return;
        }

        int len = sourceText.length();
        int end = len - 1;

        if (isSpaceNoNewline(sourceText.charAt(end)) == true)
        {
            int first = firstTrailingSpaceNoNewlineIndex(sourceText);
            int[] lc = lineColAt(sourceText, first);

            dc.reportAt(Rules.BLANKEOF, Severity.ERROR, lc[0], lc[1], "The source file must not end with whitespace.", "whitespace");
            return;
        }

        int i = len;

        while (i > 0 && isSpaceNoNewline(sourceText.charAt(i - 1)) == true)
        {
            i--;
        }

        if (i > 0 && sourceText.charAt(i - 1) == '\n')
        {
            int j = i - 1;

            if (j > 0 && sourceText.charAt(j - 1) == '\r')
            {
                j--;
            }

            while (j > 0 && isSpaceNoNewline(sourceText.charAt(j - 1)) == true)
            {
                j--;
            }

            if (j > 0 && sourceText.charAt(j - 1) == '\n')
            {
                int[] lc = lineColAt(sourceText, j);
                dc.reportAt(Rules.BLANKEOF, Severity.ERROR, lc[0], lc[1], "The source file must not end with an empty line.", "empty line");
            }
        }
    }

    private static boolean isSpaceNoNewline(char c)
    {
        if (c == ' ')
        {
            return true;
        }

        if (c == '\t')
        {
            return true;
        }

        if (c == '\f')
        {
            return true;
        }

        return false;
    }

    private static int firstTrailingSpaceNoNewlineIndex(String s)
    {
        int i = s.length() - 1;

        while (i >= 0 && isSpaceNoNewline(s.charAt(i)) == true)
        {
            i--;
        }

        return i + 1;
    }

    private static int[] lineColAt(String s, int index)
    {
        int line = 1;
        int col = 0;

        int k = 0;

        while (k < index && k < s.length())
        {
            char c = s.charAt(k);

            if (c == '\n')
            {
                line++;
                col = 0;
            }
            else
            {
                col++;
            }

            k++;
        }

        return new int[] { line, col };
    }
}
