package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Color;
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
    private static final int ACTION_DURATION_MS = 6000;
    private static final int ANIMATION_FRAMES = 12;
    private static final int ANIMATION_FRAME_DELAY_MS = 15;

    private final JComponent content;
    private final JPanel card = new JPanel();
    private final JLabel message = new JLabel();
    private final JLabel actionLabel = new JLabel();
    private javax.swing.Timer autoHideTimer;
    private int entryAnimationCount;
    private javax.swing.Timer entryTimer;
    private int entryStartY;
    private int entryTargetY;
    private int entryFrame;
    private int currentCardY = -1;

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
        entryTargetY = Math.max(0, size.height - pref.height - MARGIN_PX);
        if (currentCardY < 0) currentCardY = entryTargetY;
        card.setBounds(MARGIN_PX, currentCardY, cardWidth, pref.height);
    }

    private void presentEntry()
    {
        if (card.isVisible())
        {
            // Replace-in-place: no animation.
            return;
        }
        entryAnimationCount++;
        Dimension size = getSize();
        Dimension pref = card.getPreferredSize();
        entryStartY = size.height; // just below the visible area
        currentCardY = entryStartY;
        card.setBounds(MARGIN_PX, currentCardY, Math.max(0, size.width - 2 * MARGIN_PX), pref.height);
        entryFrame = 0;

        if (entryTimer != null && entryTimer.isRunning()) entryTimer.stop();
        entryTimer = new javax.swing.Timer(ANIMATION_FRAME_DELAY_MS, e -> {
            entryFrame++;
            double t = Math.min(1.0, (double) entryFrame / ANIMATION_FRAMES);
            currentCardY = (int) Math.round(entryStartY + (entryTargetY - entryStartY) * t);
            card.setLocation(MARGIN_PX, currentCardY);
            if (entryFrame >= ANIMATION_FRAMES) ((javax.swing.Timer) e.getSource()).stop();
        });
        entryTimer.start();
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
        show(text, Severity.SUCCESS);
    }

    @Override
    public void show(String text, Severity severity)
    {
        applyMessage(text, severity);
        actionLabel.setVisible(false);
        positionCard();
        presentEntry();
        card.setVisible(true);
        restartAutoHide(DEFAULT_DURATION_MS);
    }

    @Override
    public void show(String text, String actionLabel, Runnable onClick)
    {
        show(text, actionLabel, onClick, Severity.SUCCESS);
    }

    @Override
    public void show(String text, String actionLabelText, Runnable onClick, Severity severity)
    {
        applyMessage(text, severity);
        this.actionLabel.setText("<html><u>" + Styles.escapeHtml(actionLabelText) + "</u></html>");
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
        presentEntry();
        card.setVisible(true);
        restartAutoHide(ACTION_DURATION_MS);
    }

    private void applyMessage(String text, Severity severity)
    {
        message.setForeground(colorFor(severity));
        String glyph = glyphFor(severity);
        message.setText("<html>" + Styles.escapeHtml(glyph + "  " + text) + "</html>");
    }

    private static Color colorFor(Severity severity)
    {
        switch (severity)
        {
            case WARN:  return ColorScheme.BRAND_ORANGE;
            case ERROR: return ColorScheme.PROGRESS_ERROR_COLOR;
            case SUCCESS:
            default:    return ColorScheme.PROGRESS_COMPLETE_COLOR;
        }
    }

    private static String glyphFor(Severity severity)
    {
        switch (severity)
        {
            case WARN:  return "⚠"; // U+26A0 warning sign
            case ERROR: return "✕"; // U+2715 multiplication X
            case SUCCESS:
            default:    return "✓"; // U+2713 check mark
        }
    }

    boolean cardIsVisibleForTest() { return card.isVisible(); }
    String cardLabelTextForTest() { return message.getText(); }
    javax.swing.Timer autoHideTimerForTest() { return autoHideTimer; }
    JLabel actionLabelForTest() { return actionLabel; }
    int entryAnimationCountForTest() { return entryAnimationCount; }

    int cardCurrentYForTest()
    {
        return card.getY();
    }

    void completeEntryAnimationForTest()
    {
        if (entryTimer == null) return;
        while (entryFrame < ANIMATION_FRAMES)
        {
            entryFrame++;
            double t = Math.min(1.0, (double) entryFrame / ANIMATION_FRAMES);
            currentCardY = (int) Math.round(entryStartY + (entryTargetY - entryStartY) * t);
            card.setLocation(MARGIN_PX, currentCardY);
        }
        entryTimer.stop();
    }
}
