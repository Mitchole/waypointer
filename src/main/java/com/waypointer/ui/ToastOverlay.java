package com.waypointer.ui;

import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;

/**
 * Floating-toast host. Wraps a piece of panel content in a JLayeredPane so a
 * confirmation card can appear over the content without consuming layout space.
 * Implements {@link Toasts} so callers depend on the small API rather than the
 * Swing internals.
 */
public final class ToastOverlay extends JLayeredPane implements Toasts
{
    private static final int MARGIN_PX = 8;

    private final JComponent content;
    private final JPanel card = new JPanel();

    public ToastOverlay(JComponent content)
    {
        this.content = content;
        setLayout(null); // JLayeredPane uses absolute positioning
        add(content, JLayeredPane.DEFAULT_LAYER);
        add(card, JLayeredPane.POPUP_LAYER);
        card.setVisible(false);
    }

    @Override
    public void doLayout()
    {
        Dimension size = getSize();
        content.setBounds(0, 0, size.width, size.height);
        // Card positioning handled when shown.
    }

    @Override
    public Dimension getPreferredSize()
    {
        return content.getPreferredSize();
    }

    @Override
    public Dimension getMinimumSize()
    {
        return content.getMinimumSize();
    }

    // Test seam: lets ToastOverlayTest assert visibility without poking the card field.
    boolean cardIsVisibleForTest()
    {
        return card.isVisible();
    }

    @Override
    public void show(String text)
    {
        // Implemented in a later task.
    }

    @Override
    public void show(String text, String actionLabel, Runnable onClick)
    {
        // Implemented in a later task.
    }
}
