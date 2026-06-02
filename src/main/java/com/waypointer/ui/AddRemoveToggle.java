package com.waypointer.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import net.runelite.client.ui.ColorScheme;

/**
 * In-place toggle for adding / removing a preset waypoint. Flips state on click without
 * rebuilding the surrounding panel. Holds the UUID of the added waypoint while in REMOVE
 * state so the remove callback can target it directly.
 */
final class AddRemoveToggle extends JLabel
{
    private static final String ADD_GLYPH = "+";
    private static final String REMOVE_GLYPH = "−"; // U+2212 minus sign

    private final Supplier<UUID> onAdd;
    private final Consumer<UUID> onRemove;

    private UUID currentId;

    AddRemoveToggle(UUID initialId, Supplier<UUID> onAdd, Consumer<UUID> onRemove)
    {
        this.onAdd = onAdd;
        this.onRemove = onRemove;
        this.currentId = initialId;

        setOpaque(true);
        setBackground(ColorScheme.DARKER_GRAY_COLOR);
        setFont(getFont().deriveFont(Font.BOLD, 14f));
        setHorizontalAlignment(SwingConstants.CENTER);
        setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(ColorScheme.DARK_GRAY_COLOR.darker(), 1),
            BorderFactory.createEmptyBorder(4, 7, 4, 7)));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        applyState(false);
        HoverHint.shared().attach(this, () -> currentId == null ? "Add waypoint" : "Remove waypoint");

        addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (!SwingUtilities.isLeftMouseButton(e)) return;
                if (currentId == null)
                {
                    currentId = onAdd.get();
                }
                else
                {
                    onRemove.accept(currentId);
                    currentId = null;
                }
                applyState(true);
            }

            @Override
            public void mouseEntered(MouseEvent e)
            {
                applyState(true);
            }

            @Override
            public void mouseExited(MouseEvent e)
            {
                applyState(false);
            }
        });
    }

    private void applyState(boolean hovering)
    {
        Color base;
        if (currentId == null)
        {
            setText(ADD_GLYPH);
            getAccessibleContext().setAccessibleName("Add to library");
            base = ColorScheme.PROGRESS_COMPLETE_COLOR;
        }
        else
        {
            setText(REMOVE_GLYPH);
            getAccessibleContext().setAccessibleName("Remove from library");
            base = ColorScheme.PROGRESS_ERROR_COLOR;
        }
        setForeground(hovering ? base.brighter() : base);
    }
}
