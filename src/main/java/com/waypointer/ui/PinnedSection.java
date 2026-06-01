package com.waypointer.ui;

import com.waypointer.model.Category;
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
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

/**
 * Synthetic top-of-panel section listing pinned waypoints. Reuses {@link WaypointRow} for
 * the rows. Differences from {@link CategorySection}: no drag source, no category-icon
 * slot, no popup menu on the header, no empty placeholder (caller skips rendering
 * when the list is empty). Rows are constructed with the drag handle disabled.
 */
public class PinnedSection extends JPanel
{
    private final JPanel body = new JPanel();
    private final JLabel headerLabel;
    private boolean collapsed;
    private final Consumer<Boolean> onCollapseChange;

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
        this.collapsed = collapsed;
        this.onCollapseChange = onCollapseChange;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        setAlignmentX(LEFT_ALIGNMENT);

        JPanel headerRow = new JPanel(new BorderLayout(4, 0));
        headerRow.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        headerRow.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
        headerRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter collapseOnClick = new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) { toggleCollapse(); }
        };
        headerRow.addMouseListener(collapseOnClick);

        headerLabel = new JLabel(headerText());
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(headerLabel.getFont().deriveFont(Font.BOLD));
        headerLabel.setOpaque(false);
        headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerLabel.addMouseListener(collapseOnClick);

        JLabel countLabel = new JLabel("(" + pinned.size() + ")");
        countLabel.setForeground(Color.LIGHT_GRAY);
        countLabel.setFont(FontManager.getRunescapeSmallFont());
        countLabel.addMouseListener(collapseOnClick);

        JPanel centerWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        centerWrap.setOpaque(false);
        centerWrap.addMouseListener(collapseOnClick);
        centerWrap.add(headerLabel);
        centerWrap.add(countLabel);
        headerRow.add(centerWrap, BorderLayout.CENTER);

        add(headerRow, BorderLayout.NORTH);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);
        body.setAlignmentX(LEFT_ALIGNMENT);

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

        body.setVisible(!collapsed);
        add(body, BorderLayout.CENTER);
    }

    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }

    private String headerText()
    {
        return (collapsed ? "▶" : "▼") + " Pinned";
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
}
