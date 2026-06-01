package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.Wilderness;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import net.runelite.client.game.SpriteManager;

/**
 * Synthetic top-of-panel section listing pinned waypoints. Reuses {@link WaypointRow} for
 * the rows. Differences from {@link CategorySection}: no drag source, no category-icon
 * slot, no popup menu on the header, no empty placeholder (caller skips rendering
 * when the list is empty). Rows are constructed with the drag handle disabled.
 */
public class PinnedSection extends CollapsibleSection
{
    public PinnedSection(
        List<Waypoint> pinned,
        int activePathTarget,
        boolean collapsed,
        Consumer<Boolean> onCollapseChange,
        BiConsumer<Waypoint, CategorySection.RowAction> onRowAction,
        Function<Waypoint, Component> inlineProvider,
        SpriteManager spriteManager,
        Function<Waypoint, Category> categoryLookup)
    {
        super(collapsed, onCollapseChange);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        JPanel headerRow = buildHeaderRow(pinned.size());
        add(headerRow, BorderLayout.NORTH);

        for (Waypoint w : pinned)
        {
            boolean active = activePathTarget != WorldPointPacker.UNDEFINED
                && w.getPackedWorldPoint() == activePathTarget;
            Category origin = categoryLookup.apply(w);
            String originName = origin == null ? null : origin.getName();
            WaypointRow row = WaypointRow.spec(w)
                .active(active)
                .pinned(true)
                .wilderness(Wilderness.isInWilderness(w.getPackedWorldPoint()))
                .dragDisabled(true)
                .onPlay(() -> onRowAction.accept(w, CategorySection.RowAction.PLAY))
                .onClickBody(() -> onRowAction.accept(w, CategorySection.RowAction.EXPAND))
                .onTogglePin(() -> onRowAction.accept(w, CategorySection.RowAction.TOGGLE_PIN))
                .onDelete(() -> onRowAction.accept(w, CategorySection.RowAction.DELETE))
                .onEnterSelectMode(() -> onRowAction.accept(w, CategorySection.RowAction.ENTER_SELECT))
                .spriteManager(spriteManager)
                .originCategoryName(originName)
                .build();
            row.setAlignmentX(LEFT_ALIGNMENT);
            body.add(row);
            if (inlineProvider != null)
            {
                Component inline = inlineProvider.apply(w);
                if (inline != null)
                {
                    if (inline instanceof JComponent)
                    {
                        ((JComponent) inline).setAlignmentX(LEFT_ALIGNMENT);
                    }
                    body.add(inline);
                }
            }
        }

        attachBody();
    }

    @Override
    protected String headerText()
    {
        return (collapsed ? "▶" : "▼") + " Pinned";
    }
}
