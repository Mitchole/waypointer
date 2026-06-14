package com.waypointer.ui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;

/**
 * Footer strip: a centered count line plus one static tip, appended as the last body child after
 * the vertical glue so on a short list the glue pins it to the bottom of the viewport and on a
 * long list it scrolls in after the last section. Height-capped like the banners so the body's
 * {@code BoxLayout(Y_AXIS)} does not stretch it. Never hidden -- renders in every state. With no
 * filter the count line shows the full library total and the non-empty category count; while a
 * search filter is active it switches to "N of M shown" plus the number of categories that match.
 */
final class FooterStrip extends JPanel
{
    // One static tip chosen once per instance (one plugin-enable). No timer, no rotation at
    // runtime. The index varies across restarts via the nanoTime seed. The tips are verified
    // against current features (export lives on the overflow menu).
    private static final String[] TIPS = {
        "Drag the dot-grip to reorder",
        "Click a row to edit it inline",
        "Open the ⋮ menu to import or export",
        "Pin a waypoint to keep it up top",
        "Use Select for bulk move and delete",
        "Right-click a waypoint for quick actions",
        "Search by name or category",
    };

    private final int tipIndex = Math.floorMod(System.nanoTime(), TIPS.length);
    private final JLabel label = new JLabel("", SwingConstants.CENTER);

    FooterStrip()
    {
        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        add(label, BorderLayout.CENTER);
    }

    // Pure count-line formatter. Package-private so it is unit-testable without constructing
    // the Swing panel. Separator is U+00B7 (middle dot).
    static String countText(int waypoints, int categories)
    {
        return waypoints + (waypoints == 1 ? " waypoint" : " waypoints")
            + " · "
            + categories + (categories == 1 ? " category" : " categories");
    }

    // Pure filtered count-line formatter: shown-of-total plus the count of categories with
    // matches. Package-private for unit tests, like countText. Separator is U+00B7.
    static String countTextFiltered(int shown, int total, int categories)
    {
        return shown + " of " + total + " shown"
            + " · "
            + categories + (categories == 1 ? " category" : " categories");
    }

    /** Refresh the label from counts the caller already computed. {@code waypoints} is the full
     * library total; {@code categories} is non-empty categories only. */
    void refresh(int waypoints, int categories)
    {
        label.setText("<html><div style='text-align:center;'>"
            + "<span style='color:" + Styles.MUTED_HEX + ";'>" + countText(waypoints, categories) + "</span><br>"
            + "<span style='color:" + Styles.FAINT_HEX + ";font-style:italic;'>" + TIPS[tipIndex] + "</span>"
            + "</div></html>");
    }

    /** Refresh the label for the filtered state. {@code shown} is the count of waypoints
     * matching the active filter; {@code total} is the full library size; {@code categories}
     * is the number of categories that have at least one match. */
    void refreshFiltered(int shown, int total, int categories)
    {
        label.setText("<html><div style='text-align:center;'>"
            + "<span style='color:" + Styles.MUTED_HEX + ";'>" + countTextFiltered(shown, total, categories) + "</span><br>"
            + "<span style='color:" + Styles.FAINT_HEX + ";font-style:italic;'>" + TIPS[tipIndex] + "</span>"
            + "</div></html>");
    }

    // Cap height to preferred so the body's BoxLayout(Y_AXIS) does not stretch the footer.
    @Override public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }
}
