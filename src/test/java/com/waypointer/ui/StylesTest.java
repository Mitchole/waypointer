package com.waypointer.ui;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class StylesTest
{
    @Test
    public void plainNameIsUnchanged()
    {
        assertEquals("vorkath", Styles.sanitizeFilenameSegment("vorkath"));
        assertEquals("Slayer Tasks", Styles.sanitizeFilenameSegment("Slayer Tasks"));
    }

    @Test
    public void illegalFilenameCharsAreStripped()
    {
        assertEquals("abc", Styles.sanitizeFilenameSegment("a/b\\c"));
        assertEquals("xy", Styles.sanitizeFilenameSegment("x*y?"));
        assertEquals("safe", Styles.sanitizeFilenameSegment("<safe>"));
        assertEquals("ab", Styles.sanitizeFilenameSegment("a:b"));
        assertEquals("ab", Styles.sanitizeFilenameSegment("a|b"));
        assertEquals("ab", Styles.sanitizeFilenameSegment("a\"b"));
    }

    @Test
    public void leadingAndTrailingDotsAndSpacesAreStripped()
    {
        assertEquals("name", Styles.sanitizeFilenameSegment("  name  "));
        assertEquals("name", Styles.sanitizeFilenameSegment("...name..."));
        assertEquals("name", Styles.sanitizeFilenameSegment(". .name. ."));
        assertEquals("name", Styles.sanitizeFilenameSegment("  ...  name  ...  "));
    }

    @Test
    public void emptyOrAllIllegalFallsBackToUntitled()
    {
        assertEquals("untitled", Styles.sanitizeFilenameSegment(""));
        assertEquals("untitled", Styles.sanitizeFilenameSegment(null));
        assertEquals("untitled", Styles.sanitizeFilenameSegment("///"));
        assertEquals("untitled", Styles.sanitizeFilenameSegment("   "));
    }
}
