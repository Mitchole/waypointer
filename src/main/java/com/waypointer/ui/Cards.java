package com.waypointer.ui;

import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
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
                // mouseExited fires when the cursor crosses into a child component too, and the
                // event point arrives in that child's coordinate space. Convert to the card's
                // space before the bounds test, or exiting via a child whose bounds overlap the
                // card origin leaves the hover tint stuck (mirrors WaypointRow's select adapter).
                java.awt.Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), card);
                if (card.contains(p)) return;
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
}
