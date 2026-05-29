package com.waypointer.ui;

import java.awt.Cursor;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;

/**
 * Search-style text field that paints a small clear glyph inside its right inset and
 * clears its contents when the glyph is clicked. The glyph appears only while the
 * field has text. Callers reserve room for it via the border's right inset; the paint
 * position and hit zone both derive from {@link #getInsets()} so any border tweak
 * keeps glyph + click in sync.
 */
class ClearableTextField extends PlaceholderTextField
{
    private static final String CLEAR_GLYPH = "✕"; // U+2715 multiplication X
    // How wide a slice of the right inset the click hit zone occupies. Sized generously
    // so the glyph remains comfortably clickable at default font metrics.
    private static final int HIT_BAND_PX = 16;

    private static final Cursor TEXT_CURSOR = Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR);
    private static final Cursor HAND_CURSOR = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR);

    ClearableTextField(String placeholder)
    {
        super(placeholder);

        addMouseMotionListener(new MouseMotionAdapter()
        {
            @Override public void mouseMoved(MouseEvent e)
            {
                setCursor(isOverClearGlyph(e) ? HAND_CURSOR : TEXT_CURSOR);
            }
        });
        addMouseListener(new MouseAdapter()
        {
            @Override public void mousePressed(MouseEvent e)
            {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (!isOverClearGlyph(e)) return;
                setText("");
                e.consume();
            }
        });
    }

    private boolean isOverClearGlyph(MouseEvent e)
    {
        if (getText().isEmpty()) return false;
        Rectangle hit = clearGlyphHitBox();
        return hit != null && hit.contains(e.getPoint());
    }

    // Hit zone occupies a HIT_BAND_PX-wide stripe at the right edge, just inside the border.
    private Rectangle clearGlyphHitBox()
    {
        Insets in = getInsets();
        int w = getWidth();
        int h = getHeight();
        int left = Math.max(in.left, w - in.right - HIT_BAND_PX);
        return new Rectangle(left, in.top, w - in.right - left, h - in.top - in.bottom);
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (getText().isEmpty()) return;

        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            FontMetrics fm = g2.getFontMetrics();
            int glyphW = fm.stringWidth(CLEAR_GLYPH);
            Insets in = getInsets();
            // Centre the glyph horizontally within the band, vertically on the baseline.
            int bandCenterX = getWidth() - in.right - HIT_BAND_PX / 2;
            int x = bandCenterX - glyphW / 2;
            int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
            g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
            g2.drawString(CLEAR_GLYPH, x, y);
        }
        finally
        {
            g2.dispose();
        }
    }
}
