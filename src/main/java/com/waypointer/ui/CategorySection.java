package com.waypointer.ui;

import com.waypointer.model.Category;
import com.waypointer.model.CategorySortMode;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.service.Wilderness;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
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

    public static Spec spec(Category category, List<Waypoint> waypoints)
    {
        return new Spec(category, waypoints);
    }

    /** Named, defaulted parameter object for {@link CategorySection}. Mirrors {@link WaypointRow.Spec}. */
    public static final class Spec
    {
        private final Category category;
        private final List<Waypoint> waypoints;
        private int activePathTarget = WorldPointPacker.UNDEFINED;
        private boolean collapsed;
        private Consumer<Boolean> onCollapseChange = c -> {};
        private BiConsumer<Waypoint, RowAction> onRowAction = (w, a) -> {};
        private Function<Waypoint, Component> inlineProvider = w -> null;
        private DragAndDropHandler dnd;
        private Actions actions;
        private SpriteManager spriteManager;
        private boolean selectMode;
        private BulkSelection selection;
        private BiConsumer<Waypoint, Boolean> onRowSelectClick = (w, sel) -> {};
        private BiConsumer<List<UUID>, Boolean> onHeaderSelectToggle = (ids, sel) -> {};

        private Spec(Category category, List<Waypoint> waypoints)
        {
            this.category = category;
            this.waypoints = waypoints;
        }

        public Spec activePathTarget(int v) { this.activePathTarget = v; return this; }
        public Spec collapsed(boolean v) { this.collapsed = v; return this; }
        public Spec onCollapseChange(Consumer<Boolean> v) { this.onCollapseChange = v; return this; }
        public Spec onRowAction(BiConsumer<Waypoint, RowAction> v) { this.onRowAction = v; return this; }
        public Spec inlineProvider(Function<Waypoint, Component> v) { this.inlineProvider = v; return this; }
        public Spec dnd(DragAndDropHandler v) { this.dnd = v; return this; }
        public Spec actions(Actions v) { this.actions = v; return this; }
        public Spec spriteManager(SpriteManager v) { this.spriteManager = v; return this; }
        public Spec selectMode(boolean v) { this.selectMode = v; return this; }
        public Spec selection(BulkSelection v) { this.selection = v; return this; }
        public Spec onRowSelectClick(BiConsumer<Waypoint, Boolean> v) { this.onRowSelectClick = v; return this; }
        public Spec onHeaderSelectToggle(BiConsumer<List<UUID>, Boolean> v) { this.onHeaderSelectToggle = v; return this; }

        public CategorySection build() { return new CategorySection(this); }
    }

    private CategorySection(Spec s)
    {
        super(s.collapsed, s.onCollapseChange);
        this.category = s.category;
        // Unpack the spec into locals so the construction logic below reads unchanged.
        List<Waypoint> waypoints = s.waypoints;
        int activePathTarget = s.activePathTarget;
        BiConsumer<Waypoint, RowAction> onRowAction = s.onRowAction;
        Function<Waypoint, Component> inlineProvider = s.inlineProvider;
        DragAndDropHandler dnd = s.dnd;
        Actions actions = s.actions;
        SpriteManager spriteManager = s.spriteManager;
        boolean selectMode = s.selectMode;
        BulkSelection selection = s.selection;
        BiConsumer<Waypoint, Boolean> onRowSelectClick = s.onRowSelectClick;
        BiConsumer<List<UUID>, Boolean> onHeaderSelectToggle = s.onHeaderSelectToggle;
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
        if (dnd != null) dnd.attachCategoryHeader(headerLabel, headerIndicator, category.getId(), this, headerRow);

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
            List<UUID> catIds = new ArrayList<>();
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

            JMenu sortBy = new JMenu("Sort by");
            CategorySortMode active =
                category.getSortMode() == null ? CategorySortMode.MANUAL : category.getSortMode();
            sortBy.add(buildSortItem("Manual",
                CategorySortMode.MANUAL, active, actions));
            sortBy.add(buildSortItem("Name (A-Z)",
                CategorySortMode.NAME, active, actions));
            sortBy.add(buildSortItem("Date added (newest first)",
                CategorySortMode.DATE_ADDED, active, actions));

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
            menuTrigger.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 6));
            menuTrigger.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            menuTrigger.getAccessibleContext().setAccessibleName("Category options");
            HoverHint.shared().attach(menuTrigger, () -> "Category options");
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
            && category.getSortMode() != CategorySortMode.MANUAL;
        boolean dragDisabled = sortDisablesDrag || dnd == null;

        for (Waypoint w : waypoints)
        {
            boolean active = activePathTarget != WorldPointPacker.UNDEFINED
                && w.getPackedWorldPoint() == activePathTarget;
            WaypointRow row = WaypointRow.spec(w)
                .active(active)
                .pinned(w.isPinned())
                .wilderness(Wilderness.isInWilderness(w.getPackedWorldPoint()))
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
            if (dnd != null) dnd.attachWaypointRow(row, row.getDragHandle(), row, w.getId(), category.getId());
            addRow(row, w, inlineProvider);
        }
        if (waypoints.isEmpty())
        {
            EmptyDropZone empty = new EmptyDropZone();
            body.add(empty);
            // When dnd == null (a search filter is active) the zone is shown for affordance
            // but not wired as a drop target, mirroring the non-empty tail zone.
            if (dnd != null) dnd.attachTailZone(empty, empty, category.getId());
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
        CategorySortMode mode,
        CategorySortMode active,
        Actions actions)
    {
        JCheckBoxMenuItem item = new JCheckBoxMenuItem(label, mode == active);
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
        final Consumer<CategorySortMode> onSetSortMode;
        final Runnable onEnterSelect;

        public Actions(Runnable onRename, Runnable onDelete, Runnable onSetIcon,
            Runnable onSetColour,
            Consumer<CategorySortMode> onSetSortMode,
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

    /**
     * Visible drop affordance shown in place of rows when a category is empty. Carries the
     * "(empty - drag here)" hint with a dashed resting border so it reads as a target, and
     * lights up with the shared orange-top-border + tint vocabulary when a waypoint is dragged
     * over it. Wired through {@link DragAndDropHandler#attachTailZone}, so a drop appends the
     * waypoint to this category -- matching {@link TailDropZone} on non-empty categories.
     */
    private static final class EmptyDropZone extends JPanel implements DropIndicatable
    {
        private final Border resting = BorderFactory.createCompoundBorder(
            BorderFactory.createDashedBorder(Styles.MUTED_TEXT),
            BorderFactory.createEmptyBorder(2, 10, 2, 4));

        EmptyDropZone()
        {
            setLayout(new BorderLayout());
            setOpaque(false);
            setAlignmentX(LEFT_ALIGNMENT);
            setBorder(resting);
            JLabel label = new JLabel("(empty - drag here)");
            label.setForeground(Styles.MUTED_TEXT);
            add(label, BorderLayout.WEST);
        }

        @Override
        public void setDropIndicator(DropIndicatorMode mode)
        {
            switch (mode)
            {
                case BORDER_AND_TINT:
                    setOpaque(true);
                    setBackground(ColorScheme.DARK_GRAY_HOVER_COLOR);
                    setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(2, 0, 0, 0, ColorScheme.BRAND_ORANGE),
                        BorderFactory.createEmptyBorder(2, 10, 2, 4)));
                    break;
                case NONE:
                case TINT:
                default:
                    setOpaque(false);
                    setBorder(resting);
                    break;
            }
            repaint();
        }

        @Override public Dimension getMaximumSize()
        {
            return Styles.capHeight(this);
        }
    }
}
