package com.waypointer.ui;

import java.awt.Color;
import javax.swing.JButton;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

public class PlayButtonTest
{
    @Test
    public void playIconButtonResting_isAGreyGlyphWithNoFill()
    {
        JButton b = new JButton("▶");
        PlayButton.style(b, false);
        assertSame(Styles.MUTED_TEXT, b.getForeground());
        assertFalse(b.isContentAreaFilled());
    }

    @Test
    public void playIconButtonActive_setsBrandOrangeBackground()
    {
        JButton b = new JButton("▶");
        PlayButton.style(b, true);
        assertSame(ColorScheme.BRAND_ORANGE, b.getBackground());
        assertSame(Color.BLACK, b.getForeground());
    }

    @Test
    public void playIconButtonActive_setsClientPropertyTrue()
    {
        JButton b = new JButton("▶");
        PlayButton.style(b, true);
        assertEquals(Boolean.TRUE, b.getClientProperty("waypointer.playActive"));
    }

    @Test
    public void playIconButtonResting_setsClientPropertyFalse()
    {
        JButton b = new JButton("▶");
        PlayButton.style(b, false);
        assertEquals(Boolean.FALSE, b.getClientProperty("waypointer.playActive"));
    }
}
