package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.Wilderness;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

/**
 * Pinned waypoints as a grid of shortcut tiles above the category cards. Clicking a tile
 * paths to it (or stops, if it is the active target); the right-click menu edits, unpins or
 * deletes. An open editor sits under the grid. The caller skips this section when nothing is
 * pinned.
 */
public class PinnedSection extends JPanel
{
    public PinnedSection(
        List<Waypoint> pinned,
        int activePathTarget,
        BiConsumer<Waypoint, CategorySection.RowAction> onRowAction,
        Function<Waypoint, Component> inlineProvider,
        SpriteManager spriteManager)
    {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(LEFT_ALIGNMENT);
        add(Styles.caption("Pinned"));

        JPanel grid = new JPanel(new GridLayout(0, 2, Styles.CARD_GAP, Styles.CARD_GAP));
        grid.setOpaque(false);
        grid.setAlignmentX(LEFT_ALIGNMENT);
        for (Waypoint w : pinned)
        {
            boolean active = activePathTarget != WorldPointPacker.UNDEFINED
                && w.getPackedWorldPoint() == activePathTarget;
            grid.add(new Tile(w, active, onRowAction, spriteManager));
        }
        add(grid);

        for (Waypoint w : pinned)
        {
            Component inline = inlineProvider.apply(w);
            if (inline == null) continue;
            if (inline instanceof JComponent) ((JComponent) inline).setAlignmentX(LEFT_ALIGNMENT);
            add(javax.swing.Box.createVerticalStrut(Styles.CARD_GAP));
            add(inline);
        }
    }

    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }

    /** One pinned waypoint. Implements the same in-place retint the rows use. */
    static final class Tile extends CardPanel implements PathTarget
    {
        private final Waypoint waypoint;
        private boolean hovered;
        private boolean active;

        Tile(Waypoint w, boolean active, BiConsumer<Waypoint, CategorySection.RowAction> onRowAction,
            SpriteManager spriteManager)
        {
            super(new BorderLayout(5, 0));
            this.waypoint = w;
            setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 5));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            JLabel star = new JLabel(Glyphs.of(Glyphs.Shape.STAR, ColorScheme.BRAND_ORANGE));
            add(star, BorderLayout.WEST);

            boolean wild = Wilderness.isInWilderness(w.getPackedWorldPoint());
            // HTML so a long name wraps onto a second line instead of being cut off.
            JLabel name = new JLabel("<html>" + com.waypointer.util.Text.escapeHtml(w.getName()) + "</html>");
            name.setForeground(wild ? new Color(220, 130, 130) : Color.WHITE);
            name.setToolTipText(WaypointRow.buildHoverTooltip(w));
            name.getAccessibleContext().setAccessibleName(
                "Path to " + WaypointRow.accessibleName(w.getName(), wild, true));
            add(name, BorderLayout.CENTER);

            JPopupMenu popup = new JPopupMenu();
            JMenuItem edit = new JMenuItem("Edit");
            edit.addActionListener(e -> onRowAction.accept(w, CategorySection.RowAction.EXPAND_PINNED));
            JMenuItem unpin = new JMenuItem("Unpin");
            unpin.addActionListener(e -> onRowAction.accept(w, CategorySection.RowAction.TOGGLE_PIN));
            JMenuItem delete = new JMenuItem("Delete");
            delete.addActionListener(e -> onRowAction.accept(w, CategorySection.RowAction.DELETE));
            popup.add(edit);
            popup.add(unpin);
            popup.addSeparator();
            popup.add(delete);
            setComponentPopupMenu(popup);
            name.setInheritsPopupMenu(true);
            star.setInheritsPopupMenu(true);

            MouseAdapter ma = new MouseAdapter()
            {
                @Override public void mouseEntered(MouseEvent e) { hovered = true; restyle(); }
                @Override public void mouseExited(MouseEvent e)
                {
                    java.awt.Point p = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), Tile.this);
                    if (contains(p)) return;
                    hovered = false;
                    restyle();
                }
                @Override public void mouseClicked(MouseEvent e)
                {
                    if (SwingUtilities.isLeftMouseButton(e)) onRowAction.accept(w, CategorySection.RowAction.PLAY);
                }
            };
            addMouseListener(ma);
            name.addMouseListener(ma);
            star.addMouseListener(ma);
            setActive(active);
        }

        @Override public int getPackedWorldPoint() { return waypoint.getPackedWorldPoint(); }

        @Override public void setActive(boolean active)
        {
            this.active = active;
            restyle();
        }

        boolean isActiveForTest() { return active; }

        private void restyle()
        {
            setBackground(active ? Styles.ACTIVE_TINT : hovered ? Styles.ROW_HOVER : Styles.CARD_BG);
            setOutline(active ? ColorScheme.BRAND_ORANGE : null);
        }

        // Two lines of text tall, whatever the name, so the grid stays even.
        @Override public Dimension getPreferredSize()
        {
            return new Dimension(super.getPreferredSize().width, 36);
        }
    }
}
