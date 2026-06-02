package com.waypointer.ui;

import com.waypointer.util.Text;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.Border;
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
    private Timer autoHideTimer;
    private int entryAnimationCount;
    private Timer entryTimer;
    private int entryStartY;
    private int entryTargetY;
    private int entryFrame;
    private int currentCardY = -1;
    private static final int PULSE_DURATION_MS = 120;
    private Timer pulseTimer;
    private int pulseCount;

    public ToastOverlay(JComponent content)
    {
        this.content = content;
        setLayout(null);
        add(content, JLayeredPane.DEFAULT_LAYER);

        card.setLayout(new BorderLayout(8, 0));
        card.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        card.setBorder(cardBorder(ColorScheme.LIGHT_GRAY_COLOR));

        message.setFont(FontManager.getRunescapeSmallFont());
        card.add(message, BorderLayout.CENTER);

        actionLabel.setForeground(ColorScheme.BRAND_ORANGE);
        actionLabel.setFont(FontManager.getRunescapeSmallFont());
        actionLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
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
            // Replace-in-place: the card stays put, so a slide would be invisible. Flash the
            // border to brand orange briefly so two quick actions don't read as one.
            pulse();
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
        entryTimer = new Timer(ANIMATION_FRAME_DELAY_MS, e -> {
            entryFrame++;
            double t = Math.min(1.0, (double) entryFrame / ANIMATION_FRAMES);
            currentCardY = (int) Math.round(entryStartY + (entryTargetY - entryStartY) * t);
            card.setLocation(MARGIN_PX, currentCardY);
            if (entryFrame >= ANIMATION_FRAMES) ((Timer) e.getSource()).stop();
        });
        entryTimer.start();
    }

    private static Border cardBorder(Color line)
    {
        return BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(line, 1),
            BorderFactory.createEmptyBorder(8, 12, 8, 12));
    }

    private void pulse()
    {
        pulseCount++;
        card.setBorder(cardBorder(ColorScheme.BRAND_ORANGE));
        if (pulseTimer != null && pulseTimer.isRunning()) pulseTimer.stop();
        pulseTimer = new Timer(PULSE_DURATION_MS, e -> card.setBorder(cardBorder(ColorScheme.LIGHT_GRAY_COLOR)));
        pulseTimer.setRepeats(false);
        pulseTimer.start();
    }

    private void restartAutoHide(int durationMs)
    {
        if (autoHideTimer != null && autoHideTimer.isRunning()) autoHideTimer.stop();
        autoHideTimer = new Timer(durationMs, e -> card.setVisible(false));
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
        this.actionLabel.setText("<html><u>" + Text.escapeHtml(actionLabelText) + "</u></html>");
        this.actionLabel.setVisible(true);

        // Replace any previous listener so a stale runnable from the prior toast doesn't fire.
        for (MouseListener l : this.actionLabel.getMouseListeners())
        {
            this.actionLabel.removeMouseListener(l);
        }
        this.actionLabel.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
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
        message.setText("<html>" + Text.escapeHtml(glyph + "  " + text) + "</html>");
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
    Timer autoHideTimerForTest() { return autoHideTimer; }
    JLabel actionLabelForTest() { return actionLabel; }
    int entryAnimationCountForTest() { return entryAnimationCount; }
    int pulseCountForTest() { return pulseCount; }

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
