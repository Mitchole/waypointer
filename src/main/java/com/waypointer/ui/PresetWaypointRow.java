package com.waypointer.ui;

import com.waypointer.preset.PresetWaypoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

final class PresetWaypointRow extends JPanel
{
    PresetWaypointRow(String category, PresetWaypoint wp,
        Consumer<PresetWaypoint> onEdit, Consumer<PresetWaypoint> onDelete)
    {
        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JLabel name = new JLabel(wp.getName());
        name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        name.setFont(FontManager.getRunescapeSmallFont());
        JLabel tile = new JLabel(String.format("(%d, %d) p%d", wp.getX(), wp.getY(), wp.getPlane()));
        tile.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        tile.setFont(FontManager.getRunescapeSmallFont());

        JLabel edit = link("Edit", ColorScheme.BRAND_ORANGE, () -> onEdit.accept(wp));
        JLabel del = link("Delete", new Color(220, 80, 80), () -> onDelete.accept(wp));

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(edit);
        right.add(del);

        add(name, BorderLayout.WEST);
        add(tile, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    private static JLabel link(String text, Color color, Runnable r)
    {
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { r.run(); }
        });
        return l;
    }
}
