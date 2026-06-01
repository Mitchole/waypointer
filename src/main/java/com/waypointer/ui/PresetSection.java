package com.waypointer.ui;

import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetWaypoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
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

    // Historical fallback width for the row label: used until componentResized fires with a
    // real container width. Derived from the old constant kept as a safety value for tests /
    // very early paint passes when getWidth() is still zero.
    private static final int FALLBACK_LABEL_WIDTH_PX = 107;
    // Row chrome reserved on either side of the label: border left (22) + border right (40)
    // + BorderLayout hgap (12) + AddRemoveToggle button (~23) + a small margin (6).
    private static final int ROW_RESERVED_PX = 22 + 40 + 12 + 23 + 6;
    private static final int MIN_LABEL_WIDTH_PX = 60;

    private final Preset preset;
    private final SpriteManager spriteManager;
    private final Map<Integer, UUID> existingIdByPacked;
    private final Function<PresetWaypoint, UUID> onAdd;
    private final Consumer<UUID> onRemove;
    private final JPanel rows = new JPanel();
    private final JLabel chevron = new JLabel(CHEVRON_COLLAPSED);
    private final List<JLabel> rowLabels = new ArrayList<>();
    private int labelWidthPx;
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

        // Recompute the row-label width as the container grows or shrinks. Drives
        // HTML wrap so descriptions stay inside the section instead of clipping at the
        // hard-coded 107 px the old constant used.
        addComponentListener(new ComponentAdapter()
        {
            @Override public void componentResized(ComponentEvent e)
            {
                int w = computeLabelWidth(getWidth());
                if (w == labelWidthPx) return;
                labelWidthPx = w;
                for (JLabel label : rowLabels) applyLabelWidth(label, labelWidthPx);
                rows.revalidate();
                rows.repaint();
            }
        });
    }

    private static int computeLabelWidth(int containerWidth)
    {
        if (containerWidth <= 0) return FALLBACK_LABEL_WIDTH_PX;
        return Math.max(MIN_LABEL_WIDTH_PX, containerWidth - ROW_RESERVED_PX);
    }

    private static void applyLabelWidth(JLabel label, int width)
    {
        if (width <= 0) return;
        View v = (View) label.getClientProperty(BasicHTML.propertyKey);
        if (v == null) return;
        v.setSize(width, 0);
        int h = (int) Math.ceil(v.getPreferredSpan(View.Y_AXIS));
        label.setPreferredSize(new Dimension(width, h));
    }

    // Cap the section to its own preferred height so the parent BoxLayout(Y_AXIS) stacks
    // sections tight instead of stretching them down the column.
    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
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
        applyExpandedState();
    }

    private void applyExpandedState()
    {
        chevron.setText(expanded ? CHEVRON_EXPANDED : CHEVRON_COLLAPSED);
        if (expanded && rows.getComponentCount() == 0)
        {
            buildRows();
        }
        rows.setVisible(expanded);
        revalidate();
        repaint();
    }

    String getPresetName() { return preset.getCategory(); }

    boolean isExpanded() { return expanded; }

    void setExpanded(boolean expand)
    {
        if (expand == expanded) return;
        expanded = expand;
        applyExpandedState();
    }

    private void buildRows()
    {
        rowLabels.clear();
        if (labelWidthPx <= 0) labelWidthPx = computeLabelWidth(getWidth());
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
                return Styles.capHeight(this);
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
        // clips the EAST button. Force the View to lay out at the current labelWidthPx,
        // then pin the label's preferredSize so BorderLayout honours the constraint. The
        // section's componentResized listener re-applies if the container resizes.
        applyLabelWidth(label, labelWidthPx);
        rowLabels.add(label);
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
