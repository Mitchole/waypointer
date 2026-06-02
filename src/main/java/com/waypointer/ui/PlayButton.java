package com.waypointer.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import net.runelite.client.ui.ColorScheme;

/** Per-row Play button styling: dark at rest, brand-orange on hover, locked orange when active. */
final class PlayButton
{
    private PlayButton() {}

    private static final String CLIENT_PROP_ACTIVE = "waypointer.playActive";
    private static final String CLIENT_PROP_HOVER_ATTACHED = "waypointer.playHoverAttached";

    // Active flag is mirrored to a client property so the hover adapter can check it without
    // closure capture.
    static void style(JButton b, boolean active)
    {
        b.putClientProperty(CLIENT_PROP_ACTIVE, active);

        // Active row shows a stop square; resting shows the play triangle. Driven here so the
        // glyph follows every retint -- including when another row is played or the path
        // auto-clears on arrival.
        b.setText(active ? "■" : "▶");

        // U+25A0 is metrically smaller than the triangle at a given size, so the stop square gets
        // a larger point size to match the play arrow's visual weight.
        b.setFont(b.getFont().deriveFont(Font.BOLD, active ? 20f : 14f));
        Dimension size = new Dimension(36, 32);
        b.setPreferredSize(size);
        b.setMinimumSize(size);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(true);

        applyResting(b, active);

        // Attach the hover adapter once. Re-invocations on the same button would
        // otherwise stack listeners.
        if (b.getClientProperty(CLIENT_PROP_HOVER_ATTACHED) == null)
        {
            b.addMouseListener(new MouseAdapter()
            {
                @Override public void mouseEntered(MouseEvent e)
                {
                    if (Boolean.TRUE.equals(b.getClientProperty(CLIENT_PROP_ACTIVE))) return;
                    applyHover(b);
                }
                @Override public void mouseExited(MouseEvent e)
                {
                    if (Boolean.TRUE.equals(b.getClientProperty(CLIENT_PROP_ACTIVE))) return;
                    applyResting(b, false);
                }
            });
            b.putClientProperty(CLIENT_PROP_HOVER_ATTACHED, Boolean.TRUE);
        }
    }

    private static void applyResting(JButton b, boolean active)
    {
        if (active)
        {
            b.setBackground(ColorScheme.BRAND_ORANGE);
            b.setForeground(Color.BLACK);
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }
        else
        {
            b.setBackground(ColorScheme.DARKER_GRAY_COLOR);
            b.setForeground(ColorScheme.BRAND_ORANGE);
            b.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR.darker(), 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)));
        }
    }

    private static void applyHover(JButton b)
    {
        b.setBackground(ColorScheme.BRAND_ORANGE);
        b.setForeground(Color.BLACK);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.BRAND_ORANGE.darker(), 1),
            BorderFactory.createEmptyBorder(4, 8, 4, 8)));
    }
}
