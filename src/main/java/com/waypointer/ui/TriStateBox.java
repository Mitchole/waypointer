package com.waypointer.ui;

import java.awt.BasicStroke;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import javax.swing.JComponent;
import net.runelite.client.ui.ColorScheme;

/** A three-state checkbox painted by hand so it stays font- and LAF-independent. */
final class TriStateBox extends JComponent
{
    enum State { UNCHECKED, CHECKED, PARTIAL }

    private static final int SIZE = 14;
    private State state = State.UNCHECKED;

    TriStateBox()
    {
        Dimension d = new Dimension(SIZE, SIZE);
        setPreferredSize(d);
        setMinimumSize(d);
        setMaximumSize(d);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setOpaque(false);
    }

    void setState(State s) { this.state = s; repaint(); }

    @Override protected void paintComponent(Graphics g)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorScheme.DARKER_GRAY_COLOR);
        g2.fillRect(0, 0, SIZE - 1, SIZE - 1);
        g2.setColor(ColorScheme.LIGHT_GRAY_COLOR);
        g2.drawRect(0, 0, SIZE - 1, SIZE - 1);
        if (state == State.CHECKED)
        {
            g2.setColor(ColorScheme.BRAND_ORANGE);
            g2.setStroke(new BasicStroke(2f));
            g2.drawLine(3, 7, 6, 10);
            g2.drawLine(6, 10, 11, 3);
        }
        else if (state == State.PARTIAL)
        {
            g2.setColor(ColorScheme.BRAND_ORANGE);
            g2.fillRect(4, 4, SIZE - 8, SIZE - 8);
        }
        g2.dispose();
    }
}
