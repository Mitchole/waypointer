package com.waypointer.ui;

import java.awt.Color;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;

/**
 * The shared visual vocabulary for drag-and-drop targets: a hover tint and a 2 px orange
 * top-accent border. Defined once here so the rows ({@link WaypointRow}), category headers,
 * and drop zones ({@link CategorySection}) cannot drift apart.
 */
final class DropIndicators
{
    private DropIndicators() {}

    /** Hover tint applied to a target the drag is currently over. */
    static final Color TINT_BG = ColorScheme.DARK_GRAY_HOVER_COLOR;

    /** The 2 px {@link ColorScheme#BRAND_ORANGE} top border, compounded over {@code resting}. */
    static Border topAccentOver(Border resting)
    {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(2, 0, 0, 0, ColorScheme.BRAND_ORANGE),
            resting == null ? BorderFactory.createEmptyBorder() : resting);
    }

    /**
     * Apply the standard three-state indicator to {@code c}, restoring {@code resting} /
     * {@code restingBg} on {@link DropIndicatorMode#NONE}. Repaints {@code c}; callers need not.
     */
    static void apply(JComponent c, DropIndicatorMode mode, Border resting, Color restingBg)
    {
        switch (mode)
        {
            case NONE:
                c.setBorder(resting);
                c.setBackground(restingBg);
                break;
            case TINT:
                c.setBackground(TINT_BG);
                break;
            case BORDER_AND_TINT:
                c.setBorder(topAccentOver(resting));
                c.setBackground(TINT_BG);
                break;
        }
        c.repaint();
    }
}
