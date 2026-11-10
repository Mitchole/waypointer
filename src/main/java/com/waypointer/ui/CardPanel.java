package com.waypointer.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LayoutManager;
import java.awt.RenderingHints;
import javax.swing.JPanel;

// A rounded block on the panel background. Everything in the sidebar that groups rows (a
// category, the trip status, a form) sits in one, so the list reads as separate objects.
class CardPanel extends JPanel
{
    private Color outline;

    CardPanel(LayoutManager layout)
    {
        super(layout);
        setOpaque(false);
        setBackground(Styles.CARD_BG);
        setAlignmentX(LEFT_ALIGNMENT);
    }

    // An outline marks the card the user is acting on: the trip in progress, an open form.
    void setOutline(Color outline)
    {
        this.outline = outline;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g)
    {
        paintCard(g, this, getBackground(), outline);
    }

    static void paintCard(Graphics g, Component c, Color fill, Color outline)
    {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(fill);
        g2.fillRoundRect(0, 0, c.getWidth(), c.getHeight(), Styles.CARD_ARC, Styles.CARD_ARC);
        if (outline != null)
        {
            g2.setColor(outline);
            g2.drawRoundRect(0, 0, c.getWidth() - 1, c.getHeight() - 1, Styles.CARD_ARC, Styles.CARD_ARC);
        }
        g2.dispose();
    }

    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }
}
