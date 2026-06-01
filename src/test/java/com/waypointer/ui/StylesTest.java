package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import javax.swing.JButton;
import javax.swing.JPanel;
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

    @Test
    public void editableMetaRow_placesNameWestDetailCenterButtonsEast()
    {
        JPanel row = new JPanel();
        Styles.editableMetaRow(row, "Vorkath", "(1, 2) p0", () -> {}, () -> {}, () -> {});

        java.awt.BorderLayout layout = (java.awt.BorderLayout) row.getLayout();
        assertEquals("Vorkath",
            ((javax.swing.JLabel) layout.getLayoutComponent(java.awt.BorderLayout.WEST)).getText());
        assertEquals("(1, 2) p0",
            ((javax.swing.JLabel) layout.getLayoutComponent(java.awt.BorderLayout.CENTER)).getText());
        JPanel east = (JPanel) layout.getLayoutComponent(java.awt.BorderLayout.EAST);
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
        JPanel east = (JPanel) ((java.awt.BorderLayout) row.getLayout())
            .getLayoutComponent(java.awt.BorderLayout.EAST);
        ((JButton) east.getComponent(0)).doClick();
        ((JButton) east.getComponent(1)).doClick();
        ((JButton) east.getComponent(2)).doClick();
        assertEquals(true, fired[0]);
        assertEquals(true, fired[1]);
        assertEquals(true, fired[2]);
    }
}
