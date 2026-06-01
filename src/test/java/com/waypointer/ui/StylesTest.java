package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

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

    @Test
    public void editableMetaRow_placesNameWestDetailCenterButtonsEast()
    {
        JPanel row = new JPanel();
        Styles.editableMetaRow(row, "Vorkath", "(1, 2) p0", () -> {}, () -> {}, () -> {});

        BorderLayout layout = (BorderLayout) row.getLayout();
        assertEquals("Vorkath",
            ((JLabel) layout.getLayoutComponent(BorderLayout.WEST)).getText());
        assertEquals("(1, 2) p0",
            ((JLabel) layout.getLayoutComponent(BorderLayout.CENTER)).getText());
        JPanel east = (JPanel) layout.getLayoutComponent(BorderLayout.EAST);
        assertEquals(3, east.getComponentCount());
        assertEquals("Go",     ((JButton) east.getComponent(0)).getText());
        assertEquals("Edit",   ((JButton) east.getComponent(1)).getText());
        assertEquals("Delete", ((JButton) east.getComponent(2)).getText());
    }

    @Test
    public void editableMetaRow_buttonsInvokeSuppliedCallbacks()
    {
        JPanel row = new JPanel();
        boolean[] fired = new boolean[3];
        Styles.editableMetaRow(row, "n", "d",
            () -> fired[0] = true, () -> fired[1] = true, () -> fired[2] = true);
        JPanel east = (JPanel) ((BorderLayout) row.getLayout())
            .getLayoutComponent(BorderLayout.EAST);
        ((JButton) east.getComponent(0)).doClick();
        ((JButton) east.getComponent(1)).doClick();
        ((JButton) east.getComponent(2)).doClick();
        assertTrue(fired[0]);
        assertTrue(fired[1]);
        assertTrue(fired[2]);
    }
}
