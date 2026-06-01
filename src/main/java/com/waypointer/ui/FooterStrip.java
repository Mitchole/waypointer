package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.service.WaypointStore;
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
 * {@code BoxLayout(Y_AXIS)} does not stretch it. Never hidden -- renders in every state. The
 * waypoint count is the full library total; the category count is non-empty categories only.
 * Neither count changes with the active search filter.
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

    private final WaypointStore store;
    private final int tipIndex = Math.floorMod(System.nanoTime(), TIPS.length);
    private final JLabel label = new JLabel("", SwingConstants.CENTER);

    FooterStrip(WaypointStore store)
    {
        this.store = store;
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

    /** Recompute the counts from the store and refresh the label. Called from each rebuild. */
    void refresh()
    {
        int waypoints = store.getLibrary().getWaypoints().size();
        int categories = 0;
        for (Category c : store.getCategoriesOrdered())
        {
            if (!store.getWaypointsInCategory(c.getId()).isEmpty()) categories++;
        }
        label.setText("<html><div style='text-align:center;'>"
            + "<span style='color:#9b9b9b;'>" + countText(waypoints, categories) + "</span><br>"
            + "<span style='color:#6e6e6e;font-style:italic;'>" + TIPS[tipIndex] + "</span>"
            + "</div></html>");
    }

    // Cap height to preferred so the body's BoxLayout(Y_AXIS) does not stretch the footer.
    @Override public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }
}
