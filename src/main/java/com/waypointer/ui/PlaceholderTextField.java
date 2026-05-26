package com.waypointer.ui;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import javax.swing.JTextField;
import net.runelite.client.ui.ColorScheme;

/**
 * JTextField that paints a faded hint over the empty, unfocused field. The hint is paint-only
 * and never enters the field's document, so {@link #getText()} stays empty until the user
 * types. Callers that filter on the field contents need no special handling.
 */
class PlaceholderTextField extends JTextField
{
    private final String placeholder;

    PlaceholderTextField(String placeholder)
    {
        this.placeholder = placeholder == null ? "" : placeholder;
        // Explicit repaint on focus change; default JTextField repaint timing is LAF-dependent
        // and the hint must vanish the instant the field gains focus.
        addFocusListener(new FocusAdapter()
        {
            @Override public void focusGained(FocusEvent e) { repaint(); }
            @Override public void focusLost(FocusEvent e)   { repaint(); }
        });
    }

    String getPlaceholder()
    {
        return placeholder;
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        super.paintComponent(g);
        if (placeholder.isEmpty() || !getText().isEmpty() || isFocusOwner())
        {
            return;
        }
        Graphics2D g2 = (Graphics2D) g.create();
        try
        {
            g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2.setFont(getFont());
            g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
            Insets in = getInsets();
            int x = in.left;
            int y = in.top + g2.getFontMetrics().getAscent();
            g2.drawString(placeholder, x, y);
        }
        finally
        {
            g2.dispose();
        }
    }
}
