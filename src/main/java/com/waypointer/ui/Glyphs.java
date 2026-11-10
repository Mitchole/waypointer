package com.waypointer.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Path2D;
import javax.swing.Icon;

// Small drawn icons. The RuneScape fonts lack most symbol glyphs, and the fallback fonts Swing
// picks for them differ per machine, so the shapes the panel leans on are painted here.
final class Glyphs
{
    private Glyphs() {}

    enum Shape { CHEVRON_DOWN, CHEVRON_RIGHT, STAR, STAR_OUTLINE, PLUS, CHECK, CROSS, SEARCH, STOP, PLAY, BACK, MENU }

    static Icon of(Shape shape, Color color)
    {
        return of(shape, color, 14);
    }

    static Icon of(Shape shape, Color color, int size)
    {
        return new Icon()
        {
            @Override public int getIconWidth() { return size; }
            @Override public int getIconHeight() { return size; }

            @Override public void paintIcon(Component c, Graphics g, int x, int y)
            {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.translate(x + size / 2, y + size / 2);
                g2.setColor(color);
                paint(g2, shape);
                g2.dispose();
            }
        };
    }

    private static void paint(Graphics2D g, Shape shape)
    {
        switch (shape)
        {
            case CHEVRON_DOWN: triangle(g, -4, -2, 4, -2, 0, 3); break;
            case CHEVRON_RIGHT: triangle(g, -2, -4, 3, 0, -2, 4); break;
            case PLAY: triangle(g, -3, -5, 5, 0, -3, 5); break;
            case BACK: triangle(g, 3, -5, -5, 0, 3, 5); break;
            case STOP: g.fillRect(-4, -4, 8, 8); break;
            case MENU:
                for (int i = -1; i <= 1; i++) g.fillRect(-1, i * 4 - 1, 2, 2);
                break;
            case PLUS:
                g.fillRect(-4, -1, 9, 2);
                g.fillRect(-1, -4, 2, 9);
                break;
            case CHECK:
                g.setStroke(new BasicStroke(1.6f));
                g.drawLine(-4, 0, -1, 3);
                g.drawLine(-1, 3, 4, -3);
                break;
            case CROSS:
                g.setStroke(new BasicStroke(1.4f));
                g.drawLine(-3, -3, 3, 3);
                g.drawLine(-3, 3, 3, -3);
                break;
            case SEARCH:
                g.setStroke(new BasicStroke(1.5f));
                g.drawOval(-5, -5, 8, 8);
                g.drawLine(2, 2, 5, 5);
                break;
            default:
                Path2D.Double p = new Path2D.Double();
                for (int i = 0; i < 10; i++)
                {
                    double r = i % 2 == 0 ? 6 : 2.6;
                    double a = -Math.PI / 2 + i * Math.PI / 5;
                    if (i == 0) p.moveTo(r * Math.cos(a), r * Math.sin(a));
                    else p.lineTo(r * Math.cos(a), r * Math.sin(a));
                }
                p.closePath();
                g.setStroke(new BasicStroke(1.1f));
                if (shape == Shape.STAR) g.fill(p); else g.draw(p);
                break;
        }
    }

    private static void triangle(Graphics2D g, double... pts)
    {
        Path2D.Double t = new Path2D.Double();
        t.moveTo(pts[0], pts[1]);
        t.lineTo(pts[2], pts[3]);
        t.lineTo(pts[4], pts[5]);
        t.closePath();
        g.fill(t);
    }
}
