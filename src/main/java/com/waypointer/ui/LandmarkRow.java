package com.waypointer.ui;

import com.waypointer.service.BboxIndex;
import com.waypointer.service.LandmarkType;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

final class LandmarkRow extends JPanel
{
    LandmarkRow(LandmarkType type, BboxIndex.Entry entry,
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

        JLabel editLink = new JLabel("Edit");
        editLink.setForeground(ColorScheme.BRAND_ORANGE);
        editLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        editLink.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e) { onEdit.accept(entry); }
        });

        JLabel del = new JLabel("Delete");
        del.setForeground(new java.awt.Color(220, 80, 80));
        del.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        del.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e) { onDelete.accept(entry); }
        });

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.add(editLink);
        right.add(del);

        add(name, BorderLayout.WEST);
        add(tileLabel, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }
}
