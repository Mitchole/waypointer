package com.waypointer.ui;

import com.waypointer.service.LandmarkType;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.function.BiConsumer;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Inline picker shown below the {@link NearestLandmarkBar} when the customize button is
 * clicked. One row per landmark type. Each row: drag-handle, checkbox, icon, name.
 * Drag-and-drop is wired in a later task; for now the drag-handle is purely visual.
 */
final class ConfigureLandmarksPanel extends JPanel
{
    private static final int ROW_HEIGHT = 24;

    private final SpriteManager spriteManager;
    private final Map<LandmarkType, Integer> spriteIds;
    private final BiConsumer<LandmarkType, Boolean> onToggle;
    private final BiConsumer<Integer, Integer> onReorder;

    private LandmarkSelection selection;

    ConfigureLandmarksPanel(SpriteManager spriteManager,
        Map<LandmarkType, Integer> spriteIds,
        LandmarkSelection initial,
        BiConsumer<LandmarkType, Boolean> onToggle,
        BiConsumer<Integer, Integer> onReorder)
    {
        this.spriteManager = spriteManager;
        this.spriteIds = spriteIds;
        this.selection = initial;
        this.onToggle = onToggle;
        this.onReorder = onReorder;

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setVisible(false);

        rebuild();
    }

    void setSelection(LandmarkSelection s)
    {
        this.selection = s;
        rebuild();
    }

    private void rebuild()
    {
        removeAll();
        int index = 0;
        for (LandmarkType type : selection.order())
        {
            add(buildRow(type, index));
            index++;
        }
        revalidate();
        repaint();
    }

    private JPanel buildRow(LandmarkType type, int index)
    {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        row.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, ROW_HEIGHT));
        row.setPreferredSize(new Dimension(0, ROW_HEIGHT));

        JLabel handle = new JLabel("⋮"); // U+22EE VERTICAL ELLIPSIS -- drag-handle glyph
        handle.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        handle.setFont(FontManager.getRunescapeFont());
        handle.setPreferredSize(new Dimension(10, ROW_HEIGHT));
        // Drag listener added in a later task.
        row.add(handle);

        JCheckBox cb = new JCheckBox();
        cb.setSelected(selection.isSelected(type));
        cb.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        cb.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        cb.setFocusPainted(false);
        cb.addActionListener(e -> onToggle.accept(type, cb.isSelected()));
        row.add(cb);

        JLabel iconLbl = new JLabel();
        iconLbl.setPreferredSize(new Dimension(20, ROW_HEIGHT));
        applySprite(iconLbl, type);
        row.add(iconLbl);

        JLabel nameLbl = new JLabel(type.displayName());
        nameLbl.setForeground(ColorScheme.LIGHT_GRAY_COLOR);
        nameLbl.setFont(FontManager.getRunescapeSmallFont());
        row.add(nameLbl);

        return row;
    }

    private void applySprite(JLabel target, LandmarkType type)
    {
        Integer id = spriteIds.get(type);
        if (id == null || spriteManager == null) return;
        spriteManager.getSpriteAsync(id, 0, img -> {
            if (img == null) return;
            SwingUtilities.invokeLater(() -> {
                target.setIcon(new ImageIcon(scaleDownIfNeeded(img)));
            });
        });
    }

    private static Image scaleDownIfNeeded(BufferedImage src)
    {
        int longest = Math.max(src.getWidth(), src.getHeight());
        if (longest <= 16) return src;
        double scale = 16.0 / longest;
        return src.getScaledInstance((int) Math.round(src.getWidth() * scale),
            (int) Math.round(src.getHeight() * scale), Image.SCALE_SMOOTH);
    }
}
