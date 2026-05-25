package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Floating-toast host. Wraps a piece of panel content in a JLayeredPane so a
 * confirmation card can appear over the content without consuming layout space.
 * Implements {@link Toasts} so callers depend on the small API rather than the
 * Swing internals.
 */
public final class ToastOverlay extends JLayeredPane implements Toasts
{
    private static final int MARGIN_PX = 8;
    private static final int DEFAULT_DURATION_MS = 2500;

    private final JComponent content;
    private final JPanel card = new JPanel();
    private final JLabel message = new JLabel();
    private final JLabel actionLabel = new JLabel();
    private javax.swing.Timer autoHideTimer;

    public ToastOverlay(JComponent content)
    {
        this.content = content;
        setLayout(null);
        add(content, JLayeredPane.DEFAULT_LAYER);

        card.setLayout(new BorderLayout(8, 0));
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.LIGHT_GRAY_COLOR, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12)));

        message.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        message.setFont(FontManager.getRunescapeSmallFont());
        card.add(message, BorderLayout.CENTER);

        actionLabel.setForeground(ColorScheme.BRAND_ORANGE);
        actionLabel.setFont(FontManager.getRunescapeSmallFont());
        actionLabel.setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.HAND_CURSOR));
        actionLabel.setVisible(false);
        card.add(actionLabel, BorderLayout.EAST);

        add(card, JLayeredPane.POPUP_LAYER);
        card.setVisible(false);
    }

    @Override
    public void doLayout()
    {
        Dimension size = getSize();
        content.setBounds(0, 0, size.width, size.height);
        if (card.isVisible()) positionCard();
    }

    @Override public Dimension getPreferredSize() { return content.getPreferredSize(); }
    @Override public Dimension getMinimumSize() { return content.getMinimumSize(); }

    private void positionCard()
    {
        Dimension size = getSize();
        if (size.width <= 0 || size.height <= 0) return;
        Dimension pref = card.getPreferredSize();
        int cardWidth = Math.max(0, size.width - 2 * MARGIN_PX);
        int restingY = Math.max(0, size.height - pref.height - MARGIN_PX);
        card.setBounds(MARGIN_PX, restingY, cardWidth, pref.height);
    }

    private void restartAutoHide(int durationMs)
    {
        if (autoHideTimer != null && autoHideTimer.isRunning()) autoHideTimer.stop();
        autoHideTimer = new javax.swing.Timer(durationMs, e -> card.setVisible(false));
        autoHideTimer.setRepeats(false);
        autoHideTimer.start();
    }

    @Override
    public void show(String text)
    {
        message.setText("<html>" + Styles.escapeHtml(text) + "</html>");
        actionLabel.setVisible(false);
        positionCard();
        card.setVisible(true);
        restartAutoHide(DEFAULT_DURATION_MS);
    }

    @Override
    public void show(String text, String actionLabel, Runnable onClick)
    {
        message.setText("<html>" + Styles.escapeHtml(text) + "</html>");
        this.actionLabel.setText("<html><u>" + Styles.escapeHtml(actionLabel) + "</u></html>");
        this.actionLabel.setVisible(true);

        // Replace any previous listener so a stale runnable from the prior toast doesn't fire.
        for (java.awt.event.MouseListener l : this.actionLabel.getMouseListeners())
        {
            this.actionLabel.removeMouseListener(l);
        }
        this.actionLabel.addMouseListener(new java.awt.event.MouseAdapter()
        {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e)
            {
                if (autoHideTimer != null) autoHideTimer.stop();
                card.setVisible(false);
                if (onClick != null) onClick.run();
            }
        });

        positionCard();
        card.setVisible(true);
        restartAutoHide(DEFAULT_DURATION_MS);
    }

    boolean cardIsVisibleForTest() { return card.isVisible(); }
    String cardLabelTextForTest() { return message.getText(); }
    javax.swing.Timer autoHideTimerForTest() { return autoHideTimer; }
    JLabel actionLabelForTest() { return actionLabel; }
}
