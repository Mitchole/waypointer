package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;

// Collapsible section: chevron header + body of WaypointRows. Header is a BorderLayout with
// up to three children: optional 16x16 icon on the left, title label in the center (click
// target for collapse, drag source for reorder), and a vertical ellipsis menu trigger on
// the right (rename / delete / set-icon).
public class CategorySection extends CollapsibleSection
{
    private static final int COLOUR_STRIPE_WIDTH = 3;

    private final Category category;

    public CategorySection(Category category, List<Waypoint> waypoints, int activePathTarget,
        boolean collapsed,
        Consumer<Boolean> onCollapseChange,
        BiConsumer<Waypoint, RowAction> onRowAction,
        Function<Waypoint, Component> inlineProvider,
        DragAndDropHandler dnd,
        Actions actions,
        SpriteManager spriteManager,
        boolean selectMode,
        BulkSelection selection,
        BiConsumer<Waypoint, Boolean> onRowSelectClick,
        BiConsumer<java.util.List<UUID>, Boolean> onHeaderSelectToggle)
    {
        super(collapsed, onCollapseChange);
        this.category = category;
        Integer accent = category.getColor();
        if (accent != null)
        {
            setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, COLOUR_STRIPE_WIDTH, 0, 0, new Color(accent)),
                BorderFactory.createEmptyBorder(4, 0, 4, 0)));
        }
        else
        {
            // Reserve the stripe width even when uncoloured so rows don't shift when a colour
            // is set or cleared.
            setBorder(BorderFactory.createEmptyBorder(4, COLOUR_STRIPE_WIDTH, 4, 0));
        }

        JPanel headerRow = buildHeaderRow(waypoints.size());
        final Border restingHeaderBorder = headerRow.getBorder();
        final Color restingHeaderBg = headerRow.getBackground();

        // Drag must come from the label only; on headerRow, a click on the menu trigger
        // would accidentally start a drag.
        DropIndicatable headerIndicator = mode ->
        {
            switch (mode)
            {
                case NONE:
                    headerRow.setBorder(restingHeaderBorder);
                    headerRow.setBackground(restingHeaderBg);
                    break;
                case TINT:
                    headerRow.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
                    break;
                case BORDER_AND_TINT:
                    headerRow.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(2, 0, 0, 0, ColorScheme.BRAND_ORANGE),
                        restingHeaderBorder == null
                            ? BorderFactory.createEmptyBorder()
                            : restingHeaderBorder));
                    headerRow.setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
                    break;
            }
            headerRow.repaint();
        };
        if (dnd != null) dnd.attachCategoryHeader(headerLabel, headerIndicator, category.getId(), this);

        // Optional icon on the LEFT of the header row.
        if (!selectMode && category.getIconId() != null && spriteManager != null)
        {
            JLabel iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(16, 16));
            iconLabel.setOpaque(false);
            SpriteIcons.apply(iconLabel, category.getIconId(), spriteManager);
            headerRow.add(iconLabel, BorderLayout.WEST);
        }

        // Select-mode tri-state checkbox at the header's left edge. Clicking it selects or
        // deselects all of this category's currently-visible waypoints.
        if (selectMode)
        {
            java.util.List<UUID> catIds = new java.util.ArrayList<>();
            for (Waypoint w : waypoints) catIds.add(w.getId());
            TriStateBox headerBox = new TriStateBox();
            switch (selection.categoryState(catIds))
            {
                case ALL:     headerBox.setState(TriStateBox.State.CHECKED); break;
                case PARTIAL: headerBox.setState(TriStateBox.State.PARTIAL); break;
                default:      headerBox.setState(TriStateBox.State.UNCHECKED); break;
            }
            headerBox.addMouseListener(new MouseAdapter()
            {
                @Override public void mouseClicked(MouseEvent e)
                {
                    boolean allSelected = selection.categoryState(catIds) == BulkSelection.TriState.ALL;
                    onHeaderSelectToggle.accept(catIds, !allSelected);
                }
            });
            JPanel headerWest = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
            headerWest.setOpaque(false);
            headerWest.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
            headerWest.add(headerBox);
            headerRow.add(headerWest, BorderLayout.WEST);
        }

        // Build the popup menu and the trigger glyph on the RIGHT.
        if (!category.isUncategorized())
        {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem rename = new JMenuItem("Rename");
            rename.addActionListener(e -> actions.onRename.run());
            JMenuItem setIcon = new JMenuItem("Set icon...");
            setIcon.addActionListener(e -> { if (actions.onSetIcon != null) actions.onSetIcon.run(); });
            JMenuItem setColour = new JMenuItem("Set colour...");
            setColour.addActionListener(e -> { if (actions.onSetColour != null) actions.onSetColour.run(); });

            javax.swing.JMenu sortBy = new javax.swing.JMenu("Sort by");
            com.waypointer.model.CategorySortMode active =
                category.getSortMode() == null ? com.waypointer.model.CategorySortMode.MANUAL : category.getSortMode();
            sortBy.add(buildSortItem("Manual",
                com.waypointer.model.CategorySortMode.MANUAL, active, actions));
            sortBy.add(buildSortItem("Name (A-Z)",
                com.waypointer.model.CategorySortMode.NAME, active, actions));
            sortBy.add(buildSortItem("Date added (newest first)",
                com.waypointer.model.CategorySortMode.DATE_ADDED, active, actions));

            JMenuItem selectMultiple = new JMenuItem("Select multiple");
            selectMultiple.addActionListener(e -> { if (actions.onEnterSelect != null) actions.onEnterSelect.run(); });

            JMenuItem delete = new JMenuItem("Delete category");
            delete.addActionListener(e -> actions.onDelete.run());
            menu.add(rename);
            menu.add(setIcon);
            menu.add(setColour);
            menu.add(sortBy);
            menu.addSeparator();
            menu.add(selectMultiple);
            menu.add(delete);
            // Right-click on the label still opens the popup (legacy discoverability).
            headerLabel.setComponentPopupMenu(menu);

            JLabel menuTrigger = new JLabel("⋮"); // U+22EE vertical ellipsis
            menuTrigger.setForeground(Color.LIGHT_GRAY);
            menuTrigger.setFont(menuTrigger.getFont().deriveFont(Font.BOLD));
            menuTrigger.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));
            menuTrigger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            menuTrigger.setToolTipText("Category options");
            menuTrigger.getAccessibleContext().setAccessibleName("Category options");
            menuTrigger.addMouseListener(new MouseAdapter() {
                @Override public void mouseClicked(MouseEvent e)
                {
                    menu.show(menuTrigger, 0, menuTrigger.getHeight());
                }
            });
            headerRow.add(menuTrigger, BorderLayout.EAST);
        }

        add(headerRow, BorderLayout.NORTH);

        // Hide the drag handle when the row can't actually be dragged: either the category
        // sort mode is non-MANUAL (reorder is auto), or the panel passed a null dnd because
        // a search filter is active and drop targets would refer to invisible neighbours.
        boolean sortDisablesDrag = category.getSortMode() != null
            && category.getSortMode() != com.waypointer.model.CategorySortMode.MANUAL;
        boolean dragDisabled = sortDisablesDrag || dnd == null;

        for (Waypoint w : waypoints)
        {
            boolean active = activePathTarget != WorldPointPacker.UNDEFINED
                && w.getPackedWorldPoint() == activePathTarget;
            WaypointRow row = WaypointRow.spec(w)
                .active(active)
                .pinned(w.isPinned())
                .wilderness(com.waypointer.service.Wilderness.isInWilderness(w.getPackedWorldPoint()))
                .dragDisabled(dragDisabled)
                .onPlay(() -> onRowAction.accept(w, RowAction.PLAY))
                .onClickBody(() -> onRowAction.accept(w, RowAction.EXPAND))
                .onTogglePin(() -> onRowAction.accept(w, RowAction.TOGGLE_PIN))
                .onDelete(() -> onRowAction.accept(w, RowAction.DELETE))
                .onEnterSelectMode(() -> onRowAction.accept(w, RowAction.ENTER_SELECT))
                .spriteManager(spriteManager)
                .selectMode(selectMode)
                .selected(selection.ids().contains(w.getId()))
                .onSelectClick(shift -> onRowSelectClick.accept(w, shift))
                .build();
            row.setAlignmentX(LEFT_ALIGNMENT);
            body.add(row);
            if (dnd != null) dnd.attachWaypointRow(row, row.getDragHandle(),
                row, w.getId(), category.getId());
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
        if (waypoints.isEmpty())
        {
            JLabel empty = new JLabel("(empty - drag here)");
            empty.setForeground(Color.GRAY);
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 4));
            empty.setAlignmentX(LEFT_ALIGNMENT);
            body.add(empty);
        }
        else
        {
            TailDropZone tail = new TailDropZone();
            body.add(tail);
            if (dnd != null) dnd.attachTailZone(tail, tail, category.getId());
        }
        attachBody();
    }

    @Override
    protected String headerText()
    {
        String chevron = collapsed ? "▶" : "▼";
        return chevron + " " + category.getName();
    }

    public UUID getCategoryId()
    {
        return category.getId();
    }

    private static JMenuItem buildSortItem(String label,
        com.waypointer.model.CategorySortMode mode,
        com.waypointer.model.CategorySortMode active,
        Actions actions)
    {
        javax.swing.JCheckBoxMenuItem item = new javax.swing.JCheckBoxMenuItem(label, mode == active);
        item.addActionListener(e -> {
            if (actions.onSetSortMode != null) actions.onSetSortMode.accept(mode);
        });
        return item;
    }

    /** Row-level user actions plumbed up from {@link WaypointRow} to the panel. */
    public enum RowAction { PLAY, EXPAND, DELETE, TOGGLE_PIN, ENTER_SELECT }

    /** The category-level menu actions, bundled so the constructor stays readable. */
    public static final class Actions
    {
        final Runnable onRename;
        final Runnable onDelete;
        final Runnable onSetIcon;
        final Runnable onSetColour;
        final java.util.function.Consumer<com.waypointer.model.CategorySortMode> onSetSortMode;
        final Runnable onEnterSelect;

        public Actions(Runnable onRename, Runnable onDelete, Runnable onSetIcon,
            Runnable onSetColour,
            java.util.function.Consumer<com.waypointer.model.CategorySortMode> onSetSortMode,
            Runnable onEnterSelect)
        {
            this.onRename = onRename;
            this.onDelete = onDelete;
            this.onSetIcon = onSetIcon;
            this.onSetColour = onSetColour;
            this.onSetSortMode = onSetSortMode;
            this.onEnterSelect = onEnterSelect;
        }
    }

    /**
     * Thin (~8 px) hit zone appended after the last waypoint row in an expanded section.
     * Invisible at rest; paints a 2 px top border in {@link ColorScheme#BRAND_ORANGE}
     * when {@link DropIndicatable#setDropIndicator} is called with
     * {@link DropIndicatorMode#BORDER_AND_TINT}. Mirrors the existing indicator
     * vocabulary from {@code DragAndDropHandler}.
     */
    private static final class TailDropZone extends JPanel implements DropIndicatable
    {
        private static final int HEIGHT = 8;
        private final Border resting;

        TailDropZone()
        {
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setPreferredSize(new Dimension(0, HEIGHT));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, HEIGHT));
            resting = BorderFactory.createEmptyBorder();
            setBorder(resting);
        }

        @Override
        public void setDropIndicator(DropIndicatorMode mode)
        {
            switch (mode)
            {
                case BORDER_AND_TINT:
                    setBorder(BorderFactory.createMatteBorder(2, 0, 0, 0, ColorScheme.BRAND_ORANGE));
                    break;
                case NONE:
                case TINT:
                default:
                    setBorder(resting);
                    break;
            }
            repaint();
        }
    }
}
