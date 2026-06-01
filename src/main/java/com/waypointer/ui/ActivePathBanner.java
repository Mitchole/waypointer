package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.WaypointPathfinder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Slim status strip that sits between the Waypointer panel's search bar and its scrolling
 * body. Shows "Pathing to <name>" plus a Stop button while a path is active to a saved
 * waypoint, and hides itself otherwise. Pure projection of {@link WaypointPathfinder}
 * state plus the {@code showPathingBanner} config toggle.
 */
final class ActivePathBanner extends JPanel
{
    private static final String ARROW_HEX = "#FF9040";

    private final WaypointPathfinder pathfinder;
    private final WaypointerConfig config;
    private final JLabel label = new JLabel();
    private final JButton stop = new JButton("✕ Stop");

    ActivePathBanner(WaypointPathfinder pathfinder, WaypointerConfig config)
    {
        this.pathfinder = pathfinder;
        this.config = config;

        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setAlignmentX(LEFT_ALIGNMENT);

        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        add(label, BorderLayout.CENTER);

        Styles.compactSecondaryButton(stop);
        stop.getAccessibleContext().setAccessibleName("Stop pathing");
        stop.addActionListener(e -> pathfinder.clearPath());
        add(stop, BorderLayout.EAST);

        setVisible(false);
    }

    /** Pull the latest target/name from the pathfinder, refresh the label, toggle visibility. */
    void refresh()
    {
        boolean active = pathfinder.getActiveTarget() != WorldPointPacker.UNDEFINED;
        boolean enabled = config.showPathingBanner();
        boolean visible = active && enabled;
        if (visible)
        {
            String name = pathfinder.getActiveName();
            label.setText("<html><font color='" + ARROW_HEX + "'>→</font> "
                + "Pathing to " + Styles.escapeHtml(name) + "</html>");
        }
        setVisible(visible);
    }

    // Test seam: tests read the rendered label text without poking at private fields.
    String getLabelText()
    {
        return label.getText();
    }

    // Test seam: programmatically activate the Stop button so tests can verify clearPath().
    void clickStopForTest()
    {
        stop.doClick();
    }

    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }
}
