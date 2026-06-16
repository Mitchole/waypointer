package com.waypointer.ui;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import net.runelite.client.ui.ColorScheme;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DropIndicatorsTest
{
    @Test
    public void topAccentOverWrapsRestingInACompoundBorder()
    {
        Border resting = BorderFactory.createEmptyBorder(4, 0, 4, 0);
        Border result = DropIndicators.topAccentOver(resting);
        assertTrue(result instanceof CompoundBorder);
        assertEquals(resting, ((CompoundBorder) result).getInsideBorder());
    }

    @Test
    public void topAccentOverToleratesNullResting()
    {
        assertTrue(DropIndicators.topAccentOver(null) instanceof CompoundBorder);
    }

    @Test
    public void applyNoneRestoresRestingBorderAndBackground()
    {
        JPanel p = new JPanel();
        Border resting = BorderFactory.createEmptyBorder();
        Color restingBg = ColorScheme.DARKER_GRAY_COLOR;
        DropIndicators.apply(p, DropIndicatorMode.BORDER_AND_TINT, resting, restingBg);
        DropIndicators.apply(p, DropIndicatorMode.NONE, resting, restingBg);
        assertEquals(resting, p.getBorder());
        assertEquals(restingBg, p.getBackground());
    }

    @Test
    public void applyTintSetsHoverBackground()
    {
        JPanel p = new JPanel();
        DropIndicators.apply(p, DropIndicatorMode.TINT,
            BorderFactory.createEmptyBorder(), ColorScheme.DARKER_GRAY_COLOR);
        assertEquals(ColorScheme.DARK_GRAY_HOVER_COLOR, p.getBackground());
    }
}
