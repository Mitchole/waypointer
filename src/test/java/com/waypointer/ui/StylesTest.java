package com.waypointer.ui;

import java.awt.Color;
import javax.swing.JButton;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

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

    @Test
    public void playIconButtonResting_setsDarkerGrayBackground()
    {
        JButton b = new JButton("▶");
        Styles.playIconButton(b, false);
        assertSame(ColorScheme.DARKER_GRAY_COLOR, b.getBackground());
        assertSame(ColorScheme.BRAND_ORANGE, b.getForeground());
    }

    @Test
    public void playIconButtonActive_setsBrandOrangeBackground()
    {
        JButton b = new JButton("▶");
        Styles.playIconButton(b, true);
        assertSame(ColorScheme.BRAND_ORANGE, b.getBackground());
        assertSame(Color.BLACK, b.getForeground());
    }

    @Test
    public void playIconButtonActive_setsClientPropertyTrue()
    {
        JButton b = new JButton("▶");
        Styles.playIconButton(b, true);
        assertEquals(Boolean.TRUE, b.getClientProperty("waypointer.playActive"));
    }

    @Test
    public void playIconButtonResting_setsClientPropertyFalse()
    {
        JButton b = new JButton("▶");
        Styles.playIconButton(b, false);
        assertEquals(Boolean.FALSE, b.getClientProperty("waypointer.playActive"));
    }
}
