package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import net.runelite.client.game.SpriteManager;

/**
 * A single waypoint row, rendered as a clickable card. Click body -> expand inline edit.
 * Drag handle on the left initiates drag (not part of click hit region). Single primary
 * "Play" button on the right. Custom sprite icons (when set) attach to the name label so
 * icon + text share JLabel's built-in vertical centering and align with the Play button.
 */
public class WaypointRow extends JPanel
{
    private final Waypoint waypoint;
    private final JLabel dragHandle;

    public WaypointRow(Waypoint waypoint, Runnable onPlay, Runnable onClickBody,
        Runnable onDelete, Runnable onExport, Runnable onExportFile, SpriteManager spriteManager)
    {
        this.waypoint = waypoint;
        setLayout(new BorderLayout(8, 0));
        setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        setOpaque(true);

        // Hover-clickable surface: body clicks expand the row.
        MouseAdapter ma = Cards.clickable(this, onClickBody);

        // Right-click anywhere on the row body opens a popup with export and delete items.
        // Cross-platform popup-trigger handling is provided automatically by setComponentPopupMenu.
        JPopupMenu popup = new JPopupMenu();
        JMenuItem exportItem = new JMenuItem("Export waypoint");
        exportItem.addActionListener(e -> onExport.run());
        JMenuItem exportFileItem = new JMenuItem("Export waypoint to file...");
        exportFileItem.addActionListener(e -> onExportFile.run());
        JMenuItem deleteItem = new JMenuItem("Delete");
        deleteItem.addActionListener(e -> onDelete.run());
        popup.add(exportItem);
        popup.add(exportFileItem);
        popup.addSeparator();
        popup.add(deleteItem);
        setComponentPopupMenu(popup);

        // WEST: drag handle. Cursor MOVE_CURSOR; NOT attached to ma so dragging from the
        // grip doesn't fire expand. DragAndDropHandler wires the drag externally.
        dragHandle = new JLabel("⠿"); // U+283F braille pattern dots-123456
        dragHandle.setForeground(new Color(120, 120, 120));
        dragHandle.setOpaque(false);
        dragHandle.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        dragHandle.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        dragHandle.setToolTipText("Drag to reorder");
        add(dragHandle, BorderLayout.WEST);

        // CENTER: bold name label, optionally prefixed with a 16x16 custom sprite. JLabel's
        // default vertical alignment is CENTER, so text/icon align with the Play button text.
        JLabel name = new JLabel(waypoint.getName());
        name.setToolTipText(buildHoverTooltip(waypoint));
        name.setForeground(Color.WHITE);
        name.setFont(name.getFont().deriveFont(Font.BOLD));
        if (waypoint.getIconId() != null && spriteManager != null)
        {
            name.setIconTextGap(6);
            SpriteIcons.apply(name, waypoint.getIconId(), spriteManager);
        }
        name.addMouseListener(ma);
        add(name, BorderLayout.CENTER);

        // EAST: primary Play button. Consumes its own clicks - no body-click bleed.
        JButton play = new JButton("Play");
        Styles.primaryButton(play);
        play.addActionListener(e -> onPlay.run());
        add(play, BorderLayout.EAST);
    }

    public Waypoint getWaypoint() { return waypoint; }

    public JLabel getDragHandle() { return dragHandle; }

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

    @Override public Dimension getMaximumSize()
    {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }
}
