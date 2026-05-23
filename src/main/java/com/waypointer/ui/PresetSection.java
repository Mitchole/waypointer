package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetWaypoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.plaf.basic.BasicHTML;
import javax.swing.text.View;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

// One curated set, drawn as a collapsible section: a clickable header that toggles a
// stack of waypoint rows. Starts collapsed; rows are built lazily on first expand.
class PresetSection extends JPanel
{
    // U+25B8 right-pointing triangle (collapsed), U+25BE down-pointing triangle (expanded).
    private static final String CHEVRON_COLLAPSED = "▸";
    private static final String CHEVRON_EXPANDED = "▾";

    // Width budget for the row label inside the panel: PANEL_WIDTH (225) - left padding (22)
    // - right padding (22) - button (~23) - hgap (12) - scrollbar (~17), with a small margin.
    private static final int LABEL_WIDTH_PX = 107;

    private final Preset preset;
    private final SpriteManager spriteManager;
    private final Map<Integer, UUID> existingIdByPacked;
    private final Function<PresetWaypoint, UUID> onAdd;
    private final Consumer<UUID> onRemove;
    private final JPanel rows = new JPanel();
    private final JLabel chevron = new JLabel(CHEVRON_COLLAPSED);
    private boolean expanded;

    PresetSection(Preset preset, SpriteManager spriteManager, Map<Integer, UUID> existingIdByPacked,
        Function<PresetWaypoint, UUID> onAdd, Consumer<UUID> onRemove)
    {
        this.preset = preset;
        this.spriteManager = spriteManager;
        this.existingIdByPacked = existingIdByPacked;
        this.onAdd = onAdd;
        this.onRemove = onRemove;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        add(buildHeader(), BorderLayout.NORTH);

        rows.setLayout(new BoxLayout(rows, BoxLayout.Y_AXIS));
        rows.setBackground(ColorScheme.DARK_GRAY_COLOR);
        rows.setVisible(false);
        add(rows, BorderLayout.CENTER);
    }

    // Cap the section to its own preferred height so the parent BoxLayout(Y_AXIS) stacks
    // sections tight instead of stretching them down the column.
    @Override
    public Dimension getMaximumSize()
    {
        return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
    }

    private JPanel buildHeader()
    {
        JPanel header = new JPanel(new BorderLayout(6, 0));
        header.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(7, 9, 7, 9));
        header.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        chevron.setForeground(Color.LIGHT_GRAY);

        JLabel name = new JLabel(preset.getCategory());
        name.setForeground(Color.WHITE);
        name.setFont(FontManager.getRunescapeBoldFont());

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        left.setBackground(ColorScheme.DARKER_GRAY_COLOR);
        left.add(chevron);
        if (preset.getIcon() != null)
        {
            JLabel icon = new JLabel();
            SpriteIcons.apply(icon, preset.getIcon(), spriteManager);
            left.add(icon);
        }
        left.add(name);
        header.add(left, BorderLayout.CENTER);

        JLabel count = new JLabel(String.valueOf(preset.getWaypoints().size()));
        count.setForeground(Color.LIGHT_GRAY);
        count.setFont(FontManager.getRunescapeSmallFont());
        header.add(count, BorderLayout.EAST);

        header.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                toggle();
            }
        });
        return header;
    }

    private void toggle()
    {
        expanded = !expanded;
        chevron.setText(expanded ? CHEVRON_EXPANDED : CHEVRON_COLLAPSED);
        if (expanded && rows.getComponentCount() == 0)
        {
            buildRows();
        }
        rows.setVisible(expanded);
        revalidate();
        repaint();
    }

    private void buildRows()
    {
        String desc = preset.getDescription();
        if (desc != null && !desc.trim().isEmpty())
        {
            JLabel descLabel = new JLabel("<html><div style='padding:4px 9px 4px 22px;'>"
                + Styles.escapeHtml(desc) + "</div></html>");
            descLabel.setForeground(Color.GRAY);
            descLabel.setFont(FontManager.getRunescapeSmallFont());
            descLabel.setAlignmentX(LEFT_ALIGNMENT);
            rows.add(descLabel);
        }
        for (PresetWaypoint wp : preset.getWaypoints())
        {
            JPanel row = buildRow(wp);
            row.setAlignmentX(LEFT_ALIGNMENT);
            rows.add(row);
        }
    }

    private JPanel buildRow(PresetWaypoint wp)
    {
        JPanel row = new JPanel(new BorderLayout(12, 0))
        {
            @Override
            public Dimension getMaximumSize()
            {
                return new Dimension(Integer.MAX_VALUE, getPreferredSize().height);
            }
        };
        row.setBackground(ColorScheme.DARK_GRAY_COLOR);
        row.setBorder(BorderFactory.createEmptyBorder(5, 22, 5, 40));

        String descHtml = wp.getDescription() == null || wp.getDescription().trim().isEmpty()
            ? ""
            : "<br><span style='color:#7d7d7d;'>" + Styles.escapeHtml(wp.getDescription())
                + "</span>";
        JLabel label = new JLabel("<html>"
            + Styles.escapeHtml(wp.getName()) + descHtml + "</html>");
        label.setForeground(Color.WHITE);
        // BasicHTML's <div style='width:Npx'> trick is unreliable: JLabel still reports
        // its preferredSize at the unwrapped text width, which overflows the viewport and
        // clips the EAST button. Force the View to lay out at LABEL_WIDTH_PX, then pin
        // the label's preferredSize so BorderLayout honours the constraint.
        View htmlView = (View) label.getClientProperty(BasicHTML.propertyKey);
        if (htmlView != null)
        {
            htmlView.setSize(LABEL_WIDTH_PX, 0);
            int h = (int) Math.ceil(htmlView.getPreferredSpan(View.Y_AXIS));
            label.setPreferredSize(new Dimension(LABEL_WIDTH_PX, h));
        }
        row.add(label, BorderLayout.CENTER);

        int packed = WorldPointPacker.pack(wp.getX(), wp.getY(), wp.getPlane());
        UUID existingId = existingIdByPacked.get(packed);
        AddRemoveToggle toggle = new AddRemoveToggle(
            existingId,
            () -> onAdd.apply(wp),
            id -> onRemove.accept(id));
        row.add(toggle, BorderLayout.EAST);
        return row;
    }
}
