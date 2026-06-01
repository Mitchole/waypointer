package com.waypointer.ui;

import com.waypointer.model.route.Route;
import com.waypointer.service.RoutePlaybackEngine;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/** Back / Next / Stop controls plus a step counter, visible only while a route is running. */
final class RoutePlaybackBar extends JPanel
{
    private final RoutePlaybackEngine engine;
    private final JLabel label = new JLabel();
    private final JButton back = new JButton("\u25C4");           // left-pointing triangle
    private final JButton next = new JButton("Next \u25BA");     // right-pointing triangle
    private final JButton stop = new JButton("\u25A0 Stop");     // black square

    RoutePlaybackBar(RoutePlaybackEngine engine)
    {
        this.engine = engine;
        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setAlignmentX(LEFT_ALIGNMENT);

        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        add(label, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        Styles.compactSecondaryButton(back);
        Styles.compactSecondaryButton(next);
        Styles.compactSecondaryButton(stop);
        back.getAccessibleContext().setAccessibleName("Previous route step");
        next.getAccessibleContext().setAccessibleName("Next route step");
        stop.getAccessibleContext().setAccessibleName("Stop route");
        back.addActionListener(e -> engine.back());
        next.addActionListener(e -> engine.advance());
        stop.addActionListener(e -> engine.stop());
        buttons.add(back);
        buttons.add(next);
        buttons.add(stop);
        add(buttons, BorderLayout.EAST);

        setVisible(false);
    }

    void refresh()
    {
        // Read the route once: stop() can null it on the client thread between two reads.
        Route r = engine.getActiveRoute();
        if (r != null)
        {
            String text = "Step " + (engine.getCurrentIndex() + 1) + " / " + r.getSteps().size();
            if (r.isRepeating()) text += "  Lap " + engine.getLap();
            label.setText("<html><b>" + Styles.escapeHtml(r.getName()) + "</b><br>"
                + text + "</html>");
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