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
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

// Collapsible section: chevron header + body of WaypointRows. Header is a BorderLayout with
// up to three children: optional 16x16 icon on the left, title label in the center (click
// target for collapse, drag source for reorder), and a vertical ellipsis menu trigger on
// the right (rename / delete / set-icon).
public class CategorySection extends JPanel
{
    private final Category category;
    private final JPanel body = new JPanel();
    private final JLabel headerLabel;
    private boolean collapsed;
    private final Consumer<Boolean> onCollapseChange;

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
        this.category = category;
        this.collapsed = collapsed;
        this.onCollapseChange = onCollapseChange;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        // BoxLayout in the parent horizontal-stretches us to the column width, but must not
        // centre us. Pin to the left edge.
        setAlignmentX(LEFT_ALIGNMENT);

        JPanel headerRow = new JPanel(new BorderLayout(4, 0));
        headerRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerRow.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        final Border restingHeaderBorder = headerRow.getBorder();
        final Color restingHeaderBg = headerRow.getBackground();

        MouseAdapter collapseOnClick = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggleCollapse(); }
        };

        headerRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerRow.addMouseListener(collapseOnClick);

        headerLabel = new JLabel(headerText());
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        headerLabel.setOpaque(false);
        headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerLabel.addMouseListener(collapseOnClick);

        JLabel countLabel = new JLabel("(" + waypoints.size() + ")");
        countLabel.setForeground(Color.LIGHT_GRAY);
        countLabel.setFont(FontManager.getRunescapeSmallFont());

        JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        centerWrap.setOpaque(false);
        centerWrap.addMouseListener(collapseOnClick);
        countLabel.addMouseListener(collapseOnClick);
        centerWrap.add(headerLabel);
        centerWrap.add(countLabel);
        headerRow.add(centerWrap, BorderLayout.CENTER);

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

            javax.swing.JMenu sortBy = new javax.swing.JMenu("Sort by");
            com.waypointer.model.CategorySortMode active =
                category.getSortMode() == null ? com.waypointer.model.CategorySortMode.MANUAL : category.getSortMode();
            sortBy.add(buildSortItem("Manual",
                com.waypointer.model.CategorySortMode.MANUAL, active, actions));
            sortBy.add(buildSortItem("Name (A-Z)",
                com.waypointer.model.CategorySortMode.NAME, active, actions));
            sortBy.add(buildSortItem("Date added (newest first)",
                com.waypointer.model.CategorySortMode.DATE_ADDED, active, actions));

            JMenuItem delete = new JMenuItem("Delete category");
            delete.addActionListener(e -> actions.onDelete.run());
            menu.add(rename);
            menu.add(setIcon);
            menu.add(sortBy);
            menu.addSeparator();
            menu.add(delete);
            // Right-click on the label still opens the popup (legacy discoverability).
            headerLabel.setComponentPopupMenu(menu);

            JLabel menuTrigger = new JLabel("⋮"); // U+22EE vertical ellipsis
            menuTrigger.setForeground(Color.LIGHT_GRAY);
            menuTrigger.setFont(menuTrigger.getFont().deriveFont(Font.BOLD));
            menuTrigger.setBorder(BorderFactory.createEmptyBorder(0, 6, 0, 4));
            menuTrigger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            menuTrigger.setToolTipText("Category options");
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

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        body.setAlignmentX(LEFT_ALIGNMENT);
        for (Waypoint w : waypoints)
        {
            boolean active = activePathTarget != WorldPointPacker.UNDEFINED
                && w.getPackedWorldPoint() == activePathTarget;
            WaypointRow row = new WaypointRow(
                w,
                active,
                w.isPinned(),
                com.waypointer.service.Wilderness.isInWilderness(w.getPackedWorldPoint()),
                /* dragDisabled */ dragDisabled,
                () -> onRowAction.accept(w, RowAction.PLAY),
                () -> onRowAction.accept(w, RowAction.EXPAND),
                () -> onRowAction.accept(w, RowAction.TOGGLE_PIN),
                () -> onRowAction.accept(w, RowAction.DELETE),
                spriteManager,
                /* originCategoryName */ null,
                selectMode,
                selection != null && selection.ids().contains(w.getId()),
                shift -> onRowSelectClick.accept(w, shift));
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
        body.setVisible(!collapsed);
        add(body, BorderLayout.CENTER);
    }

    // Cap vertical extent at preferred height so BoxLayout(Y_AXIS) in the parent stacks
    // sections tight instead of stretching each one to fill leftover space.
    @Override
    public Dimension getMaximumSize()
    {
        Dimension pref = getPreferredSize();
        return new Dimension(Integer.MAX_VALUE, pref.height);
    }

    public JLabel getHeaderLabel() { return headerLabel; }

    private String headerText()
    {
        String chevron = collapsed ? "▶" : "▼";
        return chevron + " " + category.getName();
    }

    private void toggleCollapse()
    {
        collapsed = !collapsed;
        body.setVisible(!collapsed);
        headerLabel.setText(headerText());
        revalidate();
        repaint();
        onCollapseChange.accept(collapsed);
    }

    public UUID getCategoryId()
    {
        return category.getId();
    }

    public boolean isCollapsed() { return collapsed; }

    /**
     * Flip collapsed state WITHOUT persisting (no {@code onCollapseChange} fired). Used by the
     * spring-loaded auto-expand during a drag; persistence happens only via
     * {@link #confirmTransientExpand()} when a drop confirms the expansion.
     */
    public void setExpandedTransient(boolean expanded)
    {
        if (collapsed == !expanded) return;
        collapsed = !expanded;
        body.setVisible(expanded);
        headerLabel.setText(headerText());
        revalidate();
        repaint();
    }

    /**
     * Promote a transient expansion to persistent: fire {@code onCollapseChange(false)} once
     * so the panel's persisted collapse map (and config) reflects the new "user wants this open"
     * state. No-op if the section is currently collapsed.
     */
    public void confirmTransientExpand()
    {
        if (!collapsed) onCollapseChange.accept(false);
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
    public enum RowAction { PLAY, EXPAND, DELETE, TOGGLE_PIN }

    /** The category-level menu actions, bundled so the constructor stays readable. */
    public static final class Actions
    {
        final Runnable onRename;
        final Runnable onDelete;
        final Runnable onSetIcon;
        final java.util.function.Consumer<com.waypointer.model.CategorySortMode> onSetSortMode;

        public Actions(Runnable onRename, Runnable onDelete, Runnable onSetIcon,
            java.util.function.Consumer<com.waypointer.model.CategorySortMode> onSetSortMode)
        {
            this.onRename = onRename;
            this.onDelete = onDelete;
            this.onSetIcon = onSetIcon;
            this.onSetSortMode = onSetSortMode;
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
