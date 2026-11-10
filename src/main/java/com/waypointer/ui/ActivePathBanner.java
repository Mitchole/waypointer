package com.waypointer.ui;

import com.waypointer.WaypointerConfig;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.Navigator;
import com.waypointer.util.Text;
import java.awt.BorderLayout;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;

/**
 * Slim status strip across the top of the tab host. Shows "Pathing to <name>" plus a Stop
 * button while a path is active, and hides itself otherwise. Suppressed for targets that have
 * a saved-waypoint row -- that row's Stop square is the indicator there -- so the banner only
 * surfaces for rowless paths (landmark bar, presets, routes, death auto-path). Pure projection
 * of {@link Navigator} state, the {@code showPathingBanner} config toggle, and the
 * "target has a row" predicate.
 */
final class ActivePathBanner extends JPanel
{
    private static final String ARROW_HEX = "#FF9040";

    private final Navigator navigator;
    private final WaypointerConfig config;
    private final java.util.function.IntPredicate targetHasRow;
    private final JLabel label = new JLabel();
    private final JButton stop = new JButton("✕ Stop");

    ActivePathBanner(Navigator navigator, WaypointerConfig config,
        java.util.function.IntPredicate targetHasRow)
    {
        this.navigator = navigator;
        this.config = config;
        this.targetHasRow = targetHasRow;

        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(6, 8, 6, 8));
        setAlignmentX(LEFT_ALIGNMENT);

        label.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        add(label, BorderLayout.CENTER);

        Styles.compactSecondaryButton(stop);
        stop.getAccessibleContext().setAccessibleName("Stop pathing");
        stop.addActionListener(e -> navigator.stop());
        add(stop, BorderLayout.EAST);

        setVisible(false);
    }

    /** Pull the latest target/name from the navigator, refresh the label, toggle visibility. */
    void refresh()
    {
        int target = navigator.activeTarget();
        boolean active = target != WorldPointPacker.UNDEFINED;
        boolean enabled = config.showPathingBanner();
        // A saved-waypoint target shows its own Stop square on the row, so the banner stands down
        // and only covers rowless paths (landmark bar, presets, routes, death auto-path).
        boolean visible = active && enabled && !targetHasRow.test(target);
        if (visible)
        {
            String name = navigator.activeName();
            label.setText("<html><font color='" + ARROW_HEX + "'>→</font> "
                + "Pathing to " + Text.escapeHtml(name) + "</html>");
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
