package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import com.waypointer.util.Text;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.border.Border;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

/**
 * A single waypoint row inside a category card. Click body -> expand inline edit. Dragging
 * the name reorders. Icon-only Play button on the right (grey at rest, orange on hover or
 * when this row is the active path target); pin and menu buttons join it while the pointer is
 * over the row. Custom sprite icons (when set) attach to the name label so icon + text share
 * JLabel's built-in vertical centering and align with the Play button.
 *
 * In select mode the left edge shows a checkbox, the right-side buttons are hidden, and a
 * body click toggles selection (shift = range) instead of expanding the inline editor.
 */
public class WaypointRow extends JPanel implements DropIndicatable, PathTarget
{
    private final Waypoint waypoint;
    static final int ROW_HEIGHT = 24;

    private final JLabel dragHandle;
    private final JLabel name;
    private final boolean wilderness;
    private final MouseAdapter hover;
    private JPanel hoverControls;
    private boolean hovered;
    private final java.awt.Color restingBackground;
    private Border prevBorder;
    // Non-null only outside select mode (select mode hides the Play button). Held so the panel can
    // retint it in place on a path-target change instead of rebuilding the whole body tree (#67).
    private JButton playButton;
    private boolean activePathState;

    /** Start building a row for {@code waypoint}. Booleans default false; callbacks default no-op. */
    public static Spec spec(Waypoint waypoint)
    {
        return new Spec(waypoint);
    }

    /** Named, defaulted parameter object for {@link WaypointRow}. */
    public static final class Spec
    {
        private final Waypoint waypoint;
        private boolean active;
        private boolean isPinned;
        private boolean isWilderness;
        private boolean dragDisabled;
        private Runnable onPlay = () -> {};
        private Runnable onClickBody = () -> {};
        private Runnable onTogglePin = () -> {};
        private Runnable onDelete = () -> {};
        private SpriteManager spriteManager;
        private String originCategoryName;
        private boolean selectMode;
        private boolean selected;
        private java.util.function.Consumer<Boolean> onSelectClick = sel -> {};
        private Runnable onEnterSelectMode = () -> {};

        private Spec(Waypoint waypoint) { this.waypoint = waypoint; }

        public Spec active(boolean v) { this.active = v; return this; }
        public Spec pinned(boolean v) { this.isPinned = v; return this; }
        public Spec wilderness(boolean v) { this.isWilderness = v; return this; }
        public Spec dragDisabled(boolean v) { this.dragDisabled = v; return this; }
        public Spec onPlay(Runnable r) { this.onPlay = r; return this; }
        public Spec onClickBody(Runnable r) { this.onClickBody = r; return this; }
        public Spec onTogglePin(Runnable r) { this.onTogglePin = r; return this; }
        public Spec onDelete(Runnable r) { this.onDelete = r; return this; }
        public Spec spriteManager(SpriteManager sm) { this.spriteManager = sm; return this; }
        public Spec originCategoryName(String n) { this.originCategoryName = n; return this; }
        public Spec selectMode(boolean v) { this.selectMode = v; return this; }
        public Spec selected(boolean v) { this.selected = v; return this; }
        public Spec onSelectClick(java.util.function.Consumer<Boolean> c) { this.onSelectClick = c; return this; }
        public Spec onEnterSelectMode(Runnable r) { this.onEnterSelectMode = r; return this; }

        public WaypointRow build() { return new WaypointRow(this); }
    }

    private WaypointRow(Spec s)
    {
        this.waypoint = s.waypoint;
        this.wilderness = s.isWilderness;
        setLayout(new BorderLayout(4, 0));
        setBorder(BorderFactory.createEmptyBorder(1, 10, 0, 6));
        setOpaque(false);
        setBackground(Styles.CARD_BG);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Hover is tracked on the row and on every child, since a child with its own listener
        // swallows the events. The exit point arrives in the child's coordinate space, so it is
        // converted before the bounds test.
        hover = new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e) { setHovered(true); }
            @Override public void mouseExited(MouseEvent e)
            {
                java.awt.Point p = SwingUtilities.convertPoint(
                    e.getComponent(), e.getPoint(), WaypointRow.this);
                if (contains(p)) return;
                setHovered(false);
            }
        };
        // Body click: in select mode toggle selection (shift = range); otherwise expand inline.
        final MouseAdapter ma = new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e) { hover.mouseEntered(e); }
            @Override public void mouseExited(MouseEvent e) { hover.mouseExited(e); }
            @Override public void mouseClicked(MouseEvent e)
            {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (s.selectMode) s.onSelectClick.accept(e.isShiftDown());
                else s.onClickBody.run();
            }
        };
        addMouseListener(ma);

        // Right-click popup (pin / delete) stays available in both modes. Outside select mode it
        // also offers "Select multiple", the way into bulk selection.
        JPopupMenu popup = new JPopupMenu();
        if (!s.selectMode)
        {
            JMenuItem selectItem = new JMenuItem("Select multiple");
            selectItem.addActionListener(e -> s.onEnterSelectMode.run());
            popup.add(selectItem);
            popup.addSeparator();
        }
        JMenuItem pinItem = new JMenuItem(s.isPinned ? "Unpin" : "Pin to top");
        pinItem.addActionListener(e -> s.onTogglePin.run());
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> s.onDelete.run());
        popup.add(pinItem);
        popup.addSeparator();
        popup.add(deleteItem);
        setComponentPopupMenu(popup);

        if (s.selectMode)
        {
            TriStateBox box = new TriStateBox();
            box.setState(s.selected ? TriStateBox.State.CHECKED : TriStateBox.State.UNCHECKED);
            box.addMouseListener(ma);
            JPanel west = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            west.setOpaque(false);
            west.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            west.add(box);
            add(west, BorderLayout.WEST);
        }

        // CENTER: the name, optionally prefixed with a 16x16 custom sprite. Outside select mode
        // it doubles as the drag handle, so a row is picked up by its name.
        name = buildNameLabel(s, ma);
        add(name, BorderLayout.CENTER);
        dragHandle = s.selectMode || s.dragDisabled ? null : name;

        this.restingBackground = getBackground();
        this.activePathState = s.active;
        applyNameColour();

        buildEastControls(s, popup);
    }

    private JLabel buildNameLabel(Spec s, MouseAdapter ma)
    {
        String displayName = (s.isWilderness ? "☠ " : "") + s.waypoint.getName();
        JLabel label = new JLabel(displayName);
        label.setToolTipText(buildHoverTooltip(s.waypoint, s.originCategoryName));
        label.getAccessibleContext().setAccessibleName(
            accessibleName(s.waypoint.getName(), s.isWilderness, s.isPinned));
        if (s.waypoint.getIconId() != null && s.spriteManager != null)
        {
            label.setIconTextGap(6);
            SpriteIcons.apply(label, s.waypoint.getIconId(), s.spriteManager);
        }
        label.addMouseListener(ma);
        return label;
    }

    // EAST: pin and menu (shown only under the pointer) and the Play button. All hidden in
    // select mode.
    private void buildEastControls(Spec s, JPopupMenu popup)
    {
        if (s.selectMode) return;
        JButton pin = Styles.iconButton(
            Glyphs.of(s.isPinned ? Glyphs.Shape.STAR : Glyphs.Shape.STAR_OUTLINE,
                s.isPinned ? ColorScheme.BRAND_ORANGE : Styles.MUTED_TEXT),
            s.isPinned ? "Unpin" : "Pin to top");
        HoverHint.shared().attach(pin, () -> s.isPinned ? "Unpin" : "Pin to top");
        pin.addActionListener(e -> s.onTogglePin.run());

        JButton menu = Styles.iconButton(Glyphs.of(Glyphs.Shape.MENU, Styles.MUTED_TEXT), "Waypoint options");
        menu.addActionListener(e -> popup.show(menu, 0, menu.getHeight()));

        JButton play = new JButton("▶"); // black right-pointing triangle
        this.playButton = play;
        PlayButton.style(play, s.active);
        HoverHint.shared().attach(play, () -> activePathState ? "Stop pathing here" : "Path to here");
        play.getAccessibleContext().setAccessibleName(playAccessibleName(s.active));
        play.addActionListener(e -> s.onPlay.run());

        hoverControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        hoverControls.setOpaque(false);
        hoverControls.add(pin);
        hoverControls.add(menu);
        hoverControls.setVisible(false);

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        eastPanel.setOpaque(false);
        eastPanel.add(hoverControls);
        eastPanel.add(play);
        for (java.awt.Component c : new java.awt.Component[]{pin, menu, play, hoverControls, eastPanel})
        {
            c.addMouseListener(hover);
        }
        add(eastPanel, BorderLayout.EAST);
    }

    private void setHovered(boolean on)
    {
        if (hovered == on) return;
        hovered = on;
        if (hoverControls != null)
        {
            hoverControls.setVisible(on);
            revalidate();
        }
        repaint();
    }

    private void applyNameColour()
    {
        name.setForeground(activePathState ? ColorScheme.BRAND_ORANGE
            : wilderness ? new Color(220, 130, 130) : Color.WHITE);
    }

    // The card behind the row supplies the background; the row adds the divider above it and
    // a rounded tint while hovered, dragged over, or being pathed to.
    @Override
    protected void paintComponent(java.awt.Graphics g)
    {
        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
            java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Styles.ROW_LINE);
        g2.fillRect(8, 0, getWidth() - 16, 1);
        Color fill = null;
        if (activePathState) fill = Styles.ACTIVE_TINT;
        else if (hovered || !getBackground().equals(restingBackground)) fill = Styles.ROW_HOVER;
        if (fill != null)
        {
            g2.setColor(fill);
            g2.fillRoundRect(2, 1, getWidth() - 4, getHeight() - 1, 8, 8);
        }
        g2.dispose();
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(super.getPreferredSize().width, ROW_HEIGHT);
    }

    /** Packed world point this row represents; used by the panel to find the active row. */
    @Override
    public int getPackedWorldPoint()
    {
        return waypoint.getPackedWorldPoint();
    }

    /**
     * Retint the Play button to reflect whether this row is the active path target, in place.
     * No-op in select mode, where the Play button is hidden. Lets the panel respond to a
     * path-target change without rebuilding the body tree (#67).
     */
    @Override
    public void setActive(boolean active)
    {
        activePathState = active;
        applyNameColour();
        repaint();
        if (playButton == null) return;
        PlayButton.style(playButton, active);
        playButton.getAccessibleContext().setAccessibleName(playAccessibleName(active));
        playButton.repaint();
    }

    // "Stop pathing to X" while this row is the active target, "Path to X" otherwise, so the
    // screen-reader name tracks the same toggle the glyph and tooltip show.
    private String playAccessibleName(boolean active)
    {
        String n = waypoint.getName();
        if (n == null || n.isEmpty())
        {
            return active ? "Stop pathing to waypoint" : "Path to waypoint";
        }
        return (active ? "Stop pathing to " : "Path to ") + n;
    }

    public JLabel getDragHandle() { return dragHandle; }

    @Override
    public void setDropIndicator(DropIndicatorMode mode)
    {
        // Lazily snapshot the resting border the first time the accent is added, so a later NONE
        // restores it. prevBorder stays null until then, which topAccentOver() tolerates.
        if (mode == DropIndicatorMode.BORDER_AND_TINT && prevBorder == null) prevBorder = getBorder();
        Border resting = prevBorder != null ? prevBorder : getBorder();
        DropIndicators.apply(this, mode, resting, restingBackground);
        if (mode == DropIndicatorMode.NONE) prevBorder = null;
    }

    /**
     * Screen-reader name for a row: the waypoint name plus parenthetical state tags so the
     * wilderness skull and pinned glyph -- which are visual-only on the label -- are spoken.
     * Mirrors the Play button, which already names its path target.
     */
    static String accessibleName(String baseName, boolean wilderness, boolean pinned)
    {
        StringBuilder sb = new StringBuilder(baseName == null ? "" : baseName);
        if (wilderness) sb.append(" (in Wilderness)");
        if (pinned) sb.append(" (pinned)");
        return sb.toString();
    }

    /**
     * Hover tooltip: name on its own when notes are empty; with notes, shows the first
     * line of notes below the name (HTML-escaped).
     */
    static String buildHoverTooltip(Waypoint w)
    {
        String name = w.getName() == null ? "" : w.getName();
        String notes = w.getNotes();
        if (notes == null || notes.isEmpty()) return name;
        int nl = notes.indexOf('\n');
        String firstLine = nl < 0 ? notes : notes.substring(0, nl);
        firstLine = firstLine.trim();
        if (firstLine.isEmpty()) return name;
        return "<html>" + Text.escapeHtml(name) + "<br><span style='color:#bbb'>"
            + Text.escapeHtml(firstLine) + "</span></html>";
    }

    /** Extended tooltip with optional origin-category line. Null origin = same as the one-arg form. */
    static String buildHoverTooltip(Waypoint w, String originCategoryName)
    {
        if (originCategoryName == null || originCategoryName.isEmpty())
        {
            return buildHoverTooltip(w);
        }
        String name = w.getName() == null ? "" : w.getName();
        String notes = w.getNotes();
        StringBuilder html = new StringBuilder("<html>").append(Text.escapeHtml(name));
        if (notes != null && !notes.isEmpty())
        {
            int nl = notes.indexOf('\n');
            String firstLine = (nl < 0 ? notes : notes.substring(0, nl)).trim();
            if (!firstLine.isEmpty())
            {
                html.append("<br><span style='color:#bbb'>")
                    .append(Text.escapeHtml(firstLine))
                    .append("</span>");
            }
        }
        html.append("<br><span style='color:#bbb'><i>in ")
            .append(Text.escapeHtml(originCategoryName))
            .append("</i></span></html>");
        return html.toString();
    }

    @Override public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }
}
