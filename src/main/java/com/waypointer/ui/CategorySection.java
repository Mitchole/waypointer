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
        SpriteManager spriteManager)
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
        if (dnd != null) dnd.attachCategoryHeader(headerLabel, headerIndicator, category.getId());

        // Optional icon on the LEFT of the header row.
        if (category.getIconId() != null && spriteManager != null)
        {
            JLabel iconLabel = new JLabel();
            iconLabel.setPreferredSize(new Dimension(16, 16));
            iconLabel.setOpaque(false);
            SpriteIcons.apply(iconLabel, category.getIconId(), spriteManager);
            headerRow.add(iconLabel, BorderLayout.WEST);
        }

        // Build the popup menu and the trigger glyph on the RIGHT.
        if (!category.isUncategorized())
        {
            JPopupMenu menu = new JPopupMenu();
            JMenuItem rename = new JMenuItem("Rename");
            rename.addActionListener(e -> actions.onRename.run());
            JMenuItem setIcon = new JMenuItem("Set icon...");
            setIcon.addActionListener(e -> { if (actions.onSetIcon != null) actions.onSetIcon.run(); });
            JMenuItem exportCat = new JMenuItem("Export category");
            exportCat.addActionListener(e -> actions.onExport.run());
            JMenuItem exportFile = new JMenuItem("Export category to file...");
            exportFile.addActionListener(e -> actions.onExportFile.run());
            JMenuItem delete = new JMenuItem("Delete category");
            delete.addActionListener(e -> actions.onDelete.run());
            menu.add(rename);
            menu.add(setIcon);
            menu.addSeparator();
            menu.add(exportCat);
            menu.add(exportFile);
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
                /* dragDisabled */ false,
                () -> onRowAction.accept(w, RowAction.PLAY),
                () -> onRowAction.accept(w, RowAction.EXPAND),
                () -> onRowAction.accept(w, RowAction.TOGGLE_PIN),
                () -> onRowAction.accept(w, RowAction.DELETE),
                () -> onRowAction.accept(w, RowAction.EXPORT),
                () -> onRowAction.accept(w, RowAction.EXPORT_FILE),
                spriteManager,
                /* originCategoryName */ null);
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
            JLabel empty = new JLabel("(empty)");
            empty.setForeground(Color.GRAY);
            empty.setOpaque(false);
            empty.setBorder(BorderFactory.createEmptyBorder(2, 12, 2, 4));
            empty.setAlignmentX(LEFT_ALIGNMENT);
            body.add(empty);
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

    /** Row-level user actions plumbed up from {@link WaypointRow} to the panel. */
    public enum RowAction { PLAY, EXPAND, DELETE, EXPORT, EXPORT_FILE, TOGGLE_PIN }

    /** The category-level menu actions, bundled so the constructor stays readable. */
    public static final class Actions
    {
        final Runnable onRename;
        final Runnable onDelete;
        final Runnable onSetIcon;
        final Runnable onExport;
        final Runnable onExportFile;

        public Actions(Runnable onRename, Runnable onDelete, Runnable onSetIcon,
            Runnable onExport, Runnable onExportFile)
        {
            this.onRename = onRename;
            this.onDelete = onDelete;
            this.onSetIcon = onSetIcon;
            this.onExport = onExport;
            this.onExportFile = onExportFile;
        }
    }
}
