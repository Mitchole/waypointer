package com.waypointer.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * A compact palette of category accent colours. Fixed swatches chosen to read against the
 * RuneLite dark panel; none is brand orange, which signals active-path status elsewhere.
 * Reports the chosen packed RGB (or null for "None") to the supplied consumer.
 */
final class ColorPalettePopup
{
    // Packed RGB. Amber here is intentionally not BRAND_ORANGE.
    static final int[] SWATCHES = {
        0xCC4040, // muted red
        0xC8922A, // amber
        0x4F9D4F, // green
        0x3FA39A, // teal
        0x4477BB, // blue
        0x8A5FB0, // purple
        0xB05592, // magenta
        0x8A8F96, // grey
    };

    private ColorPalettePopup() {}

    static JPopupMenu build(Integer current, Consumer<Integer> onPick)
    {
        JPopupMenu popup = new JPopupMenu();
        popup.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        popup.setBorder(BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR));
        for (int rgb : SWATCHES)
        {
            JMenuItem item = new JMenuItem();
            item.setOpaque(true);
            item.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            item.getAccessibleContext().setAccessibleName("Category colour " + String.format("#%06X", rgb));
            item.add(new Swatch(new Color(rgb), current != null && current == rgb));
            item.addActionListener(e -> onPick.accept(rgb));
            popup.add(item);
        }
        popup.addSeparator();
        JMenuItem none = new JMenuItem("None");
        none.setOpaque(true);
        none.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        none.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        none.setFont(FontManager.getRunescapeSmallFont());
        none.getAccessibleContext().setAccessibleName("Clear category colour");
        none.addActionListener(e -> onPick.accept(null));
        popup.add(none);
        return popup;
    }

    /** A fixed-size colour chip; outlined when it is the current selection. */
    private static final class Swatch extends JComponent
    {
        private final Color color;
        private final boolean selected;

        Swatch(Color color, boolean selected)
        {
            this.color = color;
            this.selected = selected;
            setOpaque(true);
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
            setPreferredSize(new Dimension(96, 16));
            setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        }

        @Override
        protected void paintComponent(Graphics g)
        {
            g.setColor(getBackground());
            g.fillRect(0, 0, getWidth(), getHeight());
            g.setColor(color);
            g.fillRect(2, 2, getWidth() - 4, getHeight() - 4);
            if (selected)
            {
                g.setColor(ColorScheme.LIGHT_GRAY_COLOR);
                g.drawRect(1, 1, getWidth() - 3, getHeight() - 3);
            }
        }
    }
}
