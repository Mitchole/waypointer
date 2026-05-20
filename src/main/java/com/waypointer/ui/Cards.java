package com.waypointer.ui;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import net.runelite.client.ui.ColorScheme;

// Card-row pattern helpers. A "card" is a JPanel styled as a hover-aware row:
// DARKER_GRAY background, DARK_GRAY_HOVER on hover, HAND_CURSOR, and a left-click handler.
// Swing does not propagate mouse events from a child component with no listeners up to its
// parent, so callers must attach the returned MouseAdapter to any child label that should
// also be in the click hit region.
final class Cards
{
    private Cards() {}

    static MouseAdapter clickable(JPanel card, Runnable onClick)
    {
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        MouseAdapter ma = new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e)
            {
                card.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
            }

            @Override public void mouseExited(MouseEvent e)
            {
                card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            }

            @Override public void mouseClicked(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e)) onClick.run();
            }
        };
        card.addMouseListener(ma);
        return ma;
    }

    // Wraps the card's existing border with a 3px BRAND_ORANGE matte border on the left
    // edge. Marks the primary affordance among a stack of equal-weight cards.
    static void accentStripe(JPanel card)
    {
        Border existing = card.getBorder();
        Border accent = BorderFactory.createMatteBorder(0, 3, 0, 0, ColorScheme.BRAND_ORANGE);
        card.setBorder(existing == null
            ? accent
            : BorderFactory.createCompoundBorder(accent, existing));
    }
}
