package app;

/*
 * A linting rule to ensure that the source file does not end with trailing whitespaces
 * or empty blank lines. Clean file endings prevent annoying version control diffs.
 */
public final class BlankEofRule
{
    private BlankEofRule()
    {
    }

    /*
     * Main check function. Scans the end of the source text for dangling spaces or blank lines.
     * Reports formatting errors to the DiagnosticCollector if it finds any.
     */
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

        int len = sourceText.length(); // Cache the total length of the file
        int end = len - 1; // Index of the very last character in the file

        // Check if the very last character is a space, tab, or form feed
        if (isSpaceNoNewline(sourceText.charAt(end)) == true)
        {
            int first = firstTrailingSpaceNoNewlineIndex(sourceText);

            // Convert the flat string index into a human-readable line and column
            int[] lc = lineColAt(sourceText, first);

            dc.reportAt(Rules.BlankEof, Severity.ERROR, lc[0], lc[1], "The source file must not end with whitespace.", "whitespace");
            return;
        }

        int i = len;

        // Walk backward to skip over any trailing spaces at the end of the file
        while (i > 0 && isSpaceNoNewline(sourceText.charAt(i - 1)) == true)
        {
            i--;
        }

        // If we hit a newline right after skipping the end spaces, check if the previous line is also blank
        if (i > 0 && sourceText.charAt(i - 1) == '\n')
        {
            int j = i - 1;

            // Handle Windows-style CRLF line endings by skipping the carriage return
            if (j > 0 && sourceText.charAt(j - 1) == '\r')
            {
                j--;
            }

            // Skip spaces on this second-to-last line to see if it's completely empty
            while (j > 0 && isSpaceNoNewline(sourceText.charAt(j - 1)) == true)
            {
                j--;
            }

            // If we hit another newline immediately, it means there's an entirely blank line at the EOF
            if (j > 0 && sourceText.charAt(j - 1) == '\n')
            {
                int[] lc = lineColAt(sourceText, j);
                dc.reportAt(Rules.BlankEof, Severity.ERROR, lc[0], lc[1], "The source file must not end with an empty line.", "empty line");
            }
        }
    }

    /*
     * Helper to check if a character is a horizontal space.
     * Explicitly ignores vertical line breaks like \n and \r.
     */
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

    /*
     * Walks backwards from the end of the string to find exactly where the trailing whitespace starts.
     * Used to point the error marker directly at the start of the bad spaces.
     */
    private static int firstTrailingSpaceNoNewlineIndex(String s)
    {
        int i = s.length() - 1;

        while (i >= 0 && isSpaceNoNewline(s.charAt(i)) == true)
        {
            i--;
        }

        return i + 1;
    }

    /*
     * Utility function to convert a raw string index into a line and column number.
     * Vital for pointing the user to the exact location of the linting error in their IDE/console.
     */
    private static int[] lineColAt(String s, int index)
    {
        int line = 1; // Lines are naturally 1-indexed for users
        int col = 0;  // Columns start at 0

        int k = 0;

        // Iterate through the string counting newlines until we reach the target index
        while (k < index && k < s.length())
        {
            char c = s.charAt(k);

            if (c == '\n')
            {
                line++;
                col = 0; // Reset column counter on every new line
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