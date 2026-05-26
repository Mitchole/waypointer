package com.waypointer.ui;

import com.waypointer.model.Waypoint;
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
import javax.swing.border.Border;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

/**
 * A single waypoint row, rendered as a clickable card. Click body -> expand inline edit.
 * Drag handle on the left initiates drag (not part of click hit region). Icon-only Play
 * button on the right (dark at rest, brand orange on hover or when this row is the active
 * path target). Custom sprite icons (when set) attach to the name label so icon + text
 * share JLabel's built-in vertical centering and align with the Play button.
 */
public class WaypointRow extends JPanel implements DropIndicatable
{
    private final Waypoint waypoint;
    private final JLabel dragHandle;
    private final java.awt.Color restingBackground;
    private Border prevBorder;

    public WaypointRow(Waypoint waypoint, boolean active,
        boolean isPinned, boolean isWilderness, boolean dragDisabled,
        Runnable onPlay, Runnable onClickBody, Runnable onTogglePin,
        Runnable onDelete, Runnable onExport, Runnable onExportFile,
        SpriteManager spriteManager,
        String originCategoryName)
    {
        this.waypoint = waypoint;
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setOpaque(true);

        // Hover-clickable surface: body clicks expand the row.
        MouseAdapter ma = Cards.clickable(this, onClickBody);

        // Right-click anywhere on the row body opens a popup with pin/export/delete items.
        // Cross-platform popup-trigger handling is provided automatically by setComponentPopupMenu.
        JPopupMenu popup = new JPopupMenu();
        JMenuItem pinItem = new JMenuItem(isPinned ? "Unpin" : "Pin to top");
        pinItem.addActionListener(e -> onTogglePin.run());
        JMenuItem exportItem = new JMenuItem("Export waypoint");
        exportItem.addActionListener(e -> onExport.run());
        JMenuItem exportFileItem = new JMenuItem("Export waypoint to file...");
        exportFileItem.addActionListener(e -> onExportFile.run());
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> onDelete.run());
        popup.add(pinItem);
        popup.addSeparator();
        popup.add(exportItem);
        popup.add(exportFileItem);
        popup.addSeparator();
        popup.add(deleteItem);
        setComponentPopupMenu(popup);

        // WEST: drag handle. Cursor MOVE_CURSOR; NOT attached to ma so dragging from the
        // grip doesn't fire expand. DragAndDropHandler wires the drag externally.
        // Pinned section disables drag (rows are sorted by pin order, not user-arrangeable
        // by hand), so the handle is omitted.
        if (!dragDisabled)
        {
            dragHandle = new JLabel("⠿"); // U+283F braille pattern dots-123456
            dragHandle.setForeground(new Color(120, 120, 120));
            dragHandle.setOpaque(false);
            dragHandle.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
            dragHandle.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            dragHandle.setToolTipText("Drag to reorder");
            add(dragHandle, BorderLayout.WEST);
        }
        else
        {
            dragHandle = null;
        }

        // CENTER: bold name label, optionally prefixed with a 16x16 custom sprite. JLabel's
        // default vertical alignment is CENTER, so text/icon align with the Play button text.
        // Wilderness waypoints get a skull prefix and muted red foreground.
        String displayName = (isWilderness ? "☠ " : "") + waypoint.getName();
        JLabel name = new JLabel(displayName);
        name.setToolTipText(buildHoverTooltip(waypoint, originCategoryName));
        name.setForeground(isWilderness ? new Color(220, 130, 130) : Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        if (waypoint.getIconId() != null && spriteManager != null)
        {
            name.setIconTextGap(6);
            SpriteIcons.apply(name, waypoint.getIconId(), spriteManager);
        }
        name.addMouseListener(ma);
        add(name, BorderLayout.CENTER);

        // EAST: hover-revealed overflow trigger to the left of an icon-only Play button.
        // Play styling mirrors the pathfinder state (orange-locked when this is the active
        // target). The trigger's foreground is pinned to the row's resting background so it
        // reserves layout space at all times but disappears against the row at rest; the
        // hover listener below brightens it when the cursor is anywhere on the row.
        JButton play = new JButton("▶"); // U+25B6 black right-pointing triangle
        Styles.playIconButton(play, active);
        play.setToolTipText(active ? "Pathing here" : "Path to here");
        play.addActionListener(e -> onPlay.run());

        JLabel menuTrigger = new JLabel("⋮"); // U+22EE vertical ellipsis
        menuTrigger.setFont(menuTrigger.getFont().deriveFont(Font.BOLD));
        menuTrigger.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
        menuTrigger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        menuTrigger.setToolTipText("More");
        menuTrigger.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e)
            {
                popup.show(menuTrigger, 0, menuTrigger.getHeight());
            }
        });

        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        eastPanel.setOpaque(false);
        eastPanel.add(menuTrigger);
        eastPanel.add(play);
        add(eastPanel, BorderLayout.EAST);

        this.restingBackground = getBackground();
        // Pin the trigger's resting foreground to the row background so it's invisible until
        // the row is hovered; the listener swaps in LIGHT_GRAY to reveal it.
        menuTrigger.setForeground(restingBackground);
        addMouseListener(new MouseAdapter()
        {
            @Override public void mouseEntered(MouseEvent e)
            {
                menuTrigger.setForeground(Color.LIGHT_GRAY);
            }
            @Override public void mouseExited(MouseEvent e)
            {
                menuTrigger.setForeground(restingBackground);
            }
        });
    }

    public Waypoint getWaypoint() { return waypoint; }

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
        return "<html>" + Styles.escapeHtml(name) + "<br><span style='color:#bbb'>"
            + Styles.escapeHtml(firstLine) + "</span></html>";
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
        StringBuilder html = new StringBuilder("<html>").append(Styles.escapeHtml(name));
        if (notes != null && !notes.isEmpty())
        {
            int nl = notes.indexOf('\n');
            String firstLine = (nl < 0 ? notes : notes.substring(0, nl)).trim();
            if (!firstLine.isEmpty())
            {
                html.append("<br><span style='color:#bbb'>")
                    .append(Styles.escapeHtml(firstLine))
                    .append("</span>");
            }
        }
        html.append("<br><span style='color:#bbb'><i>in ")
            .append(Styles.escapeHtml(originCategoryName))
            .append("</i></span></html>");
        return html.toString();
    }

    @Override public Dimension getMaximumSize()
    {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}
