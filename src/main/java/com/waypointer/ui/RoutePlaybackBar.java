package com.waypointer.ui;

import com.waypointer.model.route.Route;
import com.waypointer.service.RoutePlaybackEngine;
import com.waypointer.util.Text;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** The running-route card: name, step counter, progress and Back / Next / Stop. Hidden when idle. */
final class RoutePlaybackBar extends JPanel
{
    private final RoutePlaybackEngine engine;
    private final JLabel label = new JLabel();
    private final JButton back = new JButton("Back");
    private final JButton next = new JButton("Next");
    private final JButton stop = new JButton("Stop");
    private final Progress progress = new Progress();

    RoutePlaybackBar(RoutePlaybackEngine engine)
    {
        this.engine = engine;
        setLayout(new BorderLayout(0, 5));
        setOpaque(false);
        // Outer margin plus card padding; see paintComponent.
        setBorder(BorderFactory.createEmptyBorder(
            Styles.CARD_GAP + 6, Styles.SIDE_PAD + 10, 8, Styles.SIDE_PAD + 10));
        setAlignmentX(LEFT_ALIGNMENT);

        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        add(label, BorderLayout.NORTH);
        add(progress, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new java.awt.GridLayout(1, 3, 4, 0));
        buttons.setOpaque(false);
        Styles.compactSecondaryButton(back);
        Styles.primaryButton(next);
        Styles.compactSecondaryButton(stop);
        next.setBorder(back.getBorder());
        back.setIcon(Glyphs.of(Glyphs.Shape.BACK, java.awt.Color.WHITE, 12));
        next.setIcon(Glyphs.of(Glyphs.Shape.PLAY, java.awt.Color.BLACK, 12));
        next.setHorizontalTextPosition(javax.swing.SwingConstants.LEFT);
        stop.setIcon(Glyphs.of(Glyphs.Shape.STOP, Styles.DELETE_RED, 12));
        back.getAccessibleContext().setAccessibleName("Previous route step");
        next.getAccessibleContext().setAccessibleName("Next route step");
        stop.getAccessibleContext().setAccessibleName("Stop route");
        back.addActionListener(e -> engine.back());
        next.addActionListener(e -> engine.advance());
        stop.addActionListener(e -> engine.stop());
        buttons.add(back);
        buttons.add(next);
        buttons.add(stop);
        add(buttons, BorderLayout.SOUTH);

        setVisible(false);
    }

    @Override
    protected void paintComponent(java.awt.Graphics g)
    {
        Styles.paintInsetCard(g, this, Styles.SIDE_PAD, Styles.CARD_GAP, Styles.SIDE_PAD, 0, null);
    }

    /** One segment per step: green for done, orange for the current one. */
    private static final class Progress extends javax.swing.JComponent
    {
        private int total;
        private int current;

        void set(int total, int current)
        {
            this.total = total;
            this.current = current;
            repaint();
        }

        @Override public Dimension getPreferredSize() { return new Dimension(0, 5); }

        @Override protected void paintComponent(java.awt.Graphics g)
        {
            if (total <= 0) return;
            java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
            g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
                java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
            int gap = total > 20 ? 1 : 3;
            double w = (getWidth() - gap * (total - 1)) / (double) total;
            for (int i = 0; i < total; i++)
            {
                g2.setColor(i < current ? ColorScheme.PROGRESS_COMPLETE_COLOR
                    : i == current ? ColorScheme.BRAND_ORANGE : Styles.RAISED);
                int x = (int) Math.round(i * (w + gap));
                g2.fillRoundRect(x, 0, Math.max(2, (int) Math.round(w)), getHeight(), 4, 4);
            }
            g2.dispose();
        }
    }

    void refresh()
    {
        // Read the route once: stop() can null it on the client thread between two reads.
        Route r = engine.getActiveRoute();
        if (r != null)
        {
            String text = "Step " + (engine.getCurrentIndex() + 1) + " / " + r.getSteps().size();
            if (r.isRepeating()) text += "  Lap " + engine.getLap();
            label.setText("<html><b style='color:white'>" + Text.escapeHtml(r.getName()) + "</b><br>"
                + text + "</html>");
            progress.set(r.getSteps().size(), engine.getCurrentIndex());
        }
        setVisible(r != null);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }

    String getLabelTextForTest() { return label.getText(); }
    void clickNextForTest() { next.doClick(); }
    void clickBackForTest() { back.doClick(); }
    void clickStopForTest() { stop.doClick(); }
}