package com.waypointer.util;

/** Small string helpers shared across the UI: HTML escaping and filename sanitization. */
public final class Text
{
    private Text() {}

    // Returns s with characters that are illegal in filenames on Windows / macOS removed
    // (/ \ : * ? " < > |), plus leading/trailing dots and whitespace trimmed. Falls back to
    // "untitled" when the input is null, empty, or reduces to nothing.
    public static String sanitizeFilenameSegment(String s)
    {
        if (s == null) return "untitled";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            switch (ch)
            {
                case '/': case '\\': case ':': case '*': case '?':
                case '"': case '<': case '>': case '|':
                    continue;
                default:
                    b.append(ch);
            }
        }
        // Strip outer whitespace and dots, alternating until stable. A single pass misses
        // inputs like ". .name. ." where dots and spaces interleave at the boundary.
        String result = b.toString();
        String prev;
        do
        {
            prev = result;
            result = result.trim();
            int start = 0, end = result.length();
            while (start < end && result.charAt(start) == '.') start++;
            while (end > start && result.charAt(end - 1) == '.') end--;
            result = result.substring(start, end);
        } while (!result.equals(prev));
        return result.isEmpty() ? "untitled" : result;
    }

    // Escapes s for embedding in HTML. Handles <, >, &, ", and '. Returns "" when s is null.
    public static String escapeHtml(String s)
    {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++)
        {
            char ch = s.charAt(i);
            switch (ch)
            {
                case '<':  b.append("&lt;");   break;
                case '>':  b.append("&gt;");   break;
                case '&':  b.append("&amp;");  break;
                case '"':  b.append("&quot;"); break;
                case '\'': b.append("&#39;");  break;
                default:   b.append(ch);
            }
        }
        return b.toString();
    }
}
