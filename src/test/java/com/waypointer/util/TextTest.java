package com.waypointer.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TextTest
{
    @Test
    public void plainNameIsUnchanged()
    {
        assertEquals("vorkath", Text.sanitizeFilenameSegment("vorkath"));
        assertEquals("Slayer Tasks", Text.sanitizeFilenameSegment("Slayer Tasks"));
    }

    @Test
    public void illegalFilenameCharsAreStripped()
    {
        assertEquals("abc", Text.sanitizeFilenameSegment("a/b\\c"));
        assertEquals("xy", Text.sanitizeFilenameSegment("x*y?"));
        assertEquals("safe", Text.sanitizeFilenameSegment("<safe>"));
        assertEquals("ab", Text.sanitizeFilenameSegment("a:b"));
        assertEquals("ab", Text.sanitizeFilenameSegment("a|b"));
        assertEquals("ab", Text.sanitizeFilenameSegment("a\"b"));
    }

    @Test
    public void leadingAndTrailingDotsAndSpacesAreStripped()
    {
        assertEquals("name", Text.sanitizeFilenameSegment("  name  "));
        assertEquals("name", Text.sanitizeFilenameSegment("...name..."));
        assertEquals("name", Text.sanitizeFilenameSegment(". .name. ."));
        assertEquals("name", Text.sanitizeFilenameSegment("  ...  name  ...  "));
    }

    @Test
    public void emptyOrAllIllegalFallsBackToUntitled()
    {
        assertEquals("untitled", Text.sanitizeFilenameSegment(""));
        assertEquals("untitled", Text.sanitizeFilenameSegment(null));
        assertEquals("untitled", Text.sanitizeFilenameSegment("///"));
        assertEquals("untitled", Text.sanitizeFilenameSegment("   "));
    }

    @Test
    public void escapeHtmlEscapesTheFiveEntities()
    {
        assertEquals("&lt;a&gt; &amp; &quot;b&quot; &#39;c&#39;",
            Text.escapeHtml("<a> & \"b\" 'c'"));
    }

    @Test
    public void escapeHtmlReturnsEmptyForNull()
    {
        assertEquals("", Text.escapeHtml(null));
    }
}
