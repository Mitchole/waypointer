package com.waypointer.ui;

import com.waypointer.preset.PresetWaypoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

final class PresetWaypointRow extends JPanel
{
    PresetWaypointRow(PresetWaypoint wp,
        Consumer<PresetWaypoint> onNavigate,
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

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(Styles.compactActionButton("Go", ColorScheme.BRAND_ORANGE, () -> onNavigate.accept(wp)));
        right.add(Styles.compactActionButton("Edit", Color.WHITE, () -> onEdit.accept(wp)));
        right.add(Styles.compactActionButton("Delete", Styles.DELETE_RED, () -> onDelete.accept(wp)));

        add(name, BorderLayout.WEST);
        add(tile, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }
}
