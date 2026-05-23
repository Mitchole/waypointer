package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Timer;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Transient confirmation strip used by {@link PresetBrowserPanel}. Shows a short
 * acknowledgement message for a fixed window, then auto-hides itself.
 */
final class ToastBar extends JPanel
{
    private static final int AUTO_HIDE_MS = 2500;

    private final JLabel message = new JLabel();
    private final Timer hideTimer;

    ToastBar()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        setAlignmentX(LEFT_ALIGNMENT);

        message.setForeground(ColorScheme.PROGRESS_COMPLETE_COLOR);
        message.setFont(FontManager.getRunescapeSmallFont());
        add(message, BorderLayout.WEST);

        hideTimer = new Timer(AUTO_HIDE_MS, e -> setVisible(false));
        hideTimer.setRepeats(false);

        setVisible(false);
    }

    /** Set the message text, make the strip visible, and restart the auto-hide timer. */
    void show(String text)
    {
        message.setText("<html>✓ " + Styles.escapeHtml(text) + "</html>");
        setVisible(true);
        hideTimer.restart();
    }

    // Test seam: tests read the rendered label text without poking at private fields.
    String getMessageText()
    {
        return message.getText();
    }

    @Override
    public Dimension getMaximumSize()
    {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }
}
