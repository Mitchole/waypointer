package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import java.awt.BorderLayout;
import java.awt.Color;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

final class LandmarkRow extends JPanel
{
    LandmarkRow(BboxIndex.Entry entry,
        Consumer<BboxIndex.Entry> onNavigate,
        Consumer<BboxIndex.Entry> onEdit,
        Consumer<BboxIndex.Entry> onDelete)
    {
        setLayout(new BorderLayout(6, 0));
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));

        JLabel name = new JLabel(entry.name);
        name.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        name.setFont(FontManager.getRunescapeSmallFont());

        String tile = entry.x1 == entry.x2 && entry.y1 == entry.y2
            ? String.format("(%d, %d) p%d", entry.x1, entry.y1, entry.plane)
            : String.format("(%d, %d)-(%d, %d) p%d", entry.x1, entry.y1, entry.x2, entry.y2, entry.plane);
        JLabel tileLabel = new JLabel(tile);
        tileLabel.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        tileLabel.setFont(FontManager.getRunescapeSmallFont());

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(Styles.compactActionButton("Go", ColorScheme.BRAND_ORANGE, () -> onNavigate.accept(entry)));
        right.add(Styles.compactActionButton("Edit", Color.WHITE, () -> onEdit.accept(entry)));
        right.add(Styles.compactActionButton("Delete", new Color(220, 80, 80), () -> onDelete.accept(entry)));

        add(name, BorderLayout.WEST);
        add(tileLabel, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }
}
