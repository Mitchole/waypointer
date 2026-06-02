package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import com.waypointer.util.Text;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
 * A single waypoint row, rendered as a clickable card. Click body -> expand inline edit.
 * Drag handle on the left initiates drag (not part of click hit region). Icon-only Play
 * button on the right (dark at rest, brand orange on hover or when this row is the active
 * path target). Custom sprite icons (when set) attach to the name label so icon + text
 * share JLabel's built-in vertical centering and align with the Play button.
 *
 * In select mode the left edge shows a checkbox instead of the drag handle, the right-side
 * Play button is hidden, and a body click toggles selection (shift = range) instead of
 * expanding the inline editor.
 */
public class WaypointRow extends JPanel implements DropIndicatable
{
    private final Waypoint waypoint;
    private final JLabel dragHandle;
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
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setOpaque(true);

        // Body click: in select mode toggle selection (shift = range); otherwise expand inline.
        final MouseAdapter ma;
        if (s.selectMode)
        {
            setBackground(ColorScheme.DARKER_GRAY_COLOR);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            ma = new MouseAdapter()
            {
                @Override public void mouseEntered(MouseEvent e)
                {
                    setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
                }
                @Override public void mouseExited(MouseEvent e)
                {
                    // ma is also attached to child components (checkbox, name label), so the
                    // event point is in the child's coordinate space. Convert to the row's
                    // space before the bounds test, or exiting via the small checkbox leaves
                    // the hover tint stuck.
                    java.awt.Point p = SwingUtilities.convertPoint(
                        e.getComponent(), e.getPoint(), WaypointRow.this);
                    if (contains(p)) return;
                    setBackground(ColorScheme.DARKER_GRAY_COLOR);
                }
                @Override public void mouseClicked(MouseEvent e)
                {
                    if (SwingUtilities.isLeftMouseButton(e)) s.onSelectClick.accept(e.isShiftDown());
                }
            };
            addMouseListener(ma);
        }
        else
        {
            ma = Cards.clickable(this, s.onClickBody);
        }

        // Right-click popup (pin / delete) stays available in both modes. Outside select mode it
        // also offers "Select multiple" -- the entry point for bulk selection now that the toolbar
        // toggle button is gone.
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

        // WEST: checkbox in select mode; drag handle otherwise (when draggable).
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
            dragHandle = null;
        }
        else if (!s.dragDisabled)
        {
            dragHandle = new JLabel("⠿"); // braille pattern dots-123456
            dragHandle.setForeground(new Color(120, 120, 120));
            dragHandle.setOpaque(false);
            dragHandle.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            dragHandle.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            dragHandle.getAccessibleContext().setAccessibleName("Reorder waypoint");
            HoverHint.shared().attach(dragHandle, () -> "Drag to reorder");
            add(dragHandle, BorderLayout.WEST);
        }
        else
        {
            dragHandle = null;
        }

        // CENTER: bold name label, optionally prefixed with a 16x16 custom sprite.
        JLabel name = buildNameLabel(s, ma);
        add(name, BorderLayout.CENTER);

        this.restingBackground = getBackground();

        // EAST: Play button, hidden in select mode. Pin / Delete are reached via right-click.
        buildEastControls(s);
    }

    private JLabel buildNameLabel(Spec s, MouseAdapter ma)
    {
        String displayName = (s.isWilderness ? "☠ " : "") + s.waypoint.getName();
        JLabel name = new JLabel(displayName);
        name.setToolTipText(buildHoverTooltip(s.waypoint, s.originCategoryName));
        name.setForeground(s.isWilderness ? new Color(220, 130, 130) : Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        name.getAccessibleContext().setAccessibleName(
            accessibleName(s.waypoint.getName(), s.isWilderness, s.isPinned));
        if (s.waypoint.getIconId() != null && s.spriteManager != null)
        {
            name.setIconTextGap(6);
            SpriteIcons.apply(name, s.waypoint.getIconId(), s.spriteManager);
        }
        name.addMouseListener(ma);
        return name;
    }

    private void buildEastControls(Spec s)
    {
        if (s.selectMode) return;
        JButton play = new JButton("▶"); // black right-pointing triangle
        this.playButton = play;
        this.activePathState = s.active;
        PlayButton.style(play, s.active);
        HoverHint.shared().attach(play, () -> activePathState ? "Pathing here" : "Path to here");
        play.getAccessibleContext().setAccessibleName(
            s.waypoint.getName() == null || s.waypoint.getName().isEmpty()
                ? "Path to waypoint"
                : "Path to " + s.waypoint.getName());
        play.addActionListener(e -> s.onPlay.run());

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        eastPanel.setOpaque(false);
        eastPanel.add(play);
        add(eastPanel, BorderLayout.EAST);
    }

    /** Packed world point this row represents; used by the panel to find the active row. */
    public int getPackedWorldPoint()
    {
        return waypoint.getPackedWorldPoint();
    }

    /**
     * Retint the Play button to reflect whether this row is the active path target, in place.
     * No-op in select mode, where the Play button is hidden. Lets the panel respond to a
     * path-target change without rebuilding the body tree (#67).
     */
    public void setActive(boolean active)
    {
        if (playButton == null) return;
        activePathState = active;
        PlayButton.style(playButton, active);
        playButton.repaint();
    }

    public JLabel getDragHandle() { return dragHandle; }

    @Override
    public void setDropIndicator(DropIndicatorMode mode)
    {
        switch (mode)
        {
            case NONE:
                if (prevBorder != null) { setBorder(prevBorder); prevBorder = null; }
                setBackground(restingBackground);
                break;
            case TINT:
                setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
                break;
            case BORDER_AND_TINT:
                if (prevBorder == null) prevBorder = getBorder();
                setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(2, 0, 0, 0, ColorScheme.BRAND_ORANGE),
                    prevBorder == null ? BorderFactory.createEmptyBorder() : prevBorder));
                setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
                break;
        }
        repaint();
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
