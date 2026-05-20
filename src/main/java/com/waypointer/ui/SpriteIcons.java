package com.waypointer.ui;

import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import net.runelite.client.game.SpriteManager;

// Process-wide ImageIcon cache keyed by sprite id. The full panel rebuild on every mutation
// creates fresh JLabels; without this cache, each row re-fires getSpriteAsync and re-allocates
// an ImageIcon for an already-decoded image. Unbounded: sprite count tops out around 1700.
final class SpriteIcons
{
    private static final Map<Integer, ImageIcon> CACHE = new ConcurrentHashMap<>();

    private SpriteIcons() {}

    // Synchronous on cache hit; on miss, falls back to spriteManager.getSpriteAsync and
    // populates the cache on success. Safe from the EDT.
    static void apply(JLabel label, int spriteId, SpriteManager spriteManager)
    {
        ImageIcon hit = CACHE.get(spriteId);
        if (hit != null)
        {
            label.setIcon(hit);
            return;
        }
        if (spriteManager == null) return;
        spriteManager.getSpriteAsync(spriteId, 0, (BufferedImage img) -> {
            if (img == null) return;
            ImageIcon icon = new ImageIcon(img);
            CACHE.put(spriteId, icon);
            SwingUtilities.invokeLater(() -> {
                label.setIcon(icon);
                label.revalidate();
                label.repaint();
            });
        });
    }
}
