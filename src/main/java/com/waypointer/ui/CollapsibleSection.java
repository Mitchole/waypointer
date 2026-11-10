package com.waypointer.ui;

import com.waypointer.model.Waypoint;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

// Base for collapsible panel sections: a clickable chevron header that toggles a vertically
// stacked body. Subclasses supply the title via headerText() and fill the protected `body`
// field; the base owns collapse state, the collapse-on-click wiring, the transient-expand
// hooks used by drag auto-expand, and the height cap that keeps a BoxLayout(Y_AXIS) parent
// from stretching each section into leftover column space.
abstract class CollapsibleSection extends JPanel
{
    protected final JPanel body = new JPanel();
    protected JLabel headerLabel;
    // The flow of title, count and any extras; and the right-hand cluster ending in the chevron.
    protected JPanel headerCenter;
    protected JPanel headerEast;
    private JLabel chevron;
    protected boolean collapsed;
    private final Consumer<Boolean> onCollapseChange;

    protected CollapsibleSection(boolean collapsed, Consumer<Boolean> onCollapseChange)
    {
        this.collapsed = collapsed;
        this.onCollapseChange = onCollapseChange;
        setLayout(new BorderLayout());
        setOpaque(false);
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        // BoxLayout in the parent horizontal-stretches us to the column width but must not
        // centre us. Pin to the left edge.
        setAlignmentX(LEFT_ALIGNMENT);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);
        body.setBorder(BorderFactory.createEmptyBorder(0, 0, 2, 0));
        body.setAlignmentX(LEFT_ALIGNMENT);
    }

    // Builds the header row: a BorderLayout panel whose CENTER holds the chevron+title label
    // and a "(count)" label, every surface wired to toggle collapse on click. Returns the row
    // so subclasses can add WEST/EAST decorations before placing it via add(..., NORTH).
    // Assigns this.headerLabel as a side effect (headerText() is read here, so subclass fields
    // it depends on must be set before calling this).
    protected JPanel buildHeaderRow(int count)
    {
        // Paints a rounded tint when a drag hovers it; otherwise the card shows through.
        JPanel headerRow = new JPanel(new BorderLayout(4, 0))
        {
            @Override protected void paintComponent(java.awt.Graphics g)
            {
                if (!getBackground().equals(Styles.CARD_BG)) CardPanel.paintCard(g, this, Styles.ROW_HOVER, null);
            }
        };
        headerRow.setOpaque(false);
        headerRow.setBackground(Styles.CARD_BG);
        headerRow.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 6));
        headerRow.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        MouseAdapter collapseOnClick = new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e) { toggleCollapse(); }
        };
        headerRow.addMouseListener(collapseOnClick);

        headerLabel = new JLabel(headerText());
        headerLabel.setForeground(Color.WHITE);
        headerLabel.setFont(FontManager.getRunescapeBoldFont());
        headerLabel.setOpaque(false);
        headerLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        headerLabel.addMouseListener(collapseOnClick);

        JLabel countLabel = Styles.pill(String.valueOf(count));
        countLabel.addMouseListener(collapseOnClick);

        headerCenter = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        headerCenter.setOpaque(false);
        headerCenter.addMouseListener(collapseOnClick);
        headerCenter.add(headerLabel);
        headerCenter.add(countLabel);
        headerRow.add(headerCenter, BorderLayout.CENTER);

        chevron = new JLabel(chevronIcon());
        chevron.addMouseListener(collapseOnClick);
        headerEast = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 0));
        headerEast.setOpaque(false);
        headerEast.addMouseListener(collapseOnClick);
        headerEast.add(chevron);
        headerRow.add(headerEast, BorderLayout.EAST);
        return headerRow;
    }

    private javax.swing.Icon chevronIcon()
    {
        return Glyphs.of(collapsed ? Glyphs.Shape.CHEVRON_RIGHT : Glyphs.Shape.CHEVRON_DOWN, Styles.FAINT_TEXT);
    }

    @Override
    protected void paintComponent(java.awt.Graphics g)
    {
        CardPanel.paintCard(g, this, Styles.CARD_BG, null);
    }

    // Places the (already-populated) body to CENTER, honouring the current collapse state.
    // Subclasses add the header to NORTH themselves (after decorating it), then call this once
    // the body rows are built.
    protected void attachBody()
    {
        body.setVisible(!collapsed);
        add(body, BorderLayout.CENTER);
    }

    protected abstract String headerText();

    protected void toggleCollapse()
    {
        collapsed = !collapsed;
        body.setVisible(!collapsed);
        headerLabel.setText(headerText());
        chevron.setIcon(chevronIcon());
        revalidate();
        repaint();
        onCollapseChange.accept(collapsed);
    }

    // Cap vertical extent at preferred height so BoxLayout(Y_AXIS) in the parent stacks
    // sections tight instead of stretching each one to fill leftover space.
    @Override
    public Dimension getMaximumSize()
    {
        return Styles.capHeight(this);
    }

    // Appends a built row plus its optional inline editor to the body, both left-aligned. Shared
    // by CategorySection and PinnedSection, which build their own WaypointRow.spec(...) (the spec
    // config differs) but stack rows identically.
    protected void addRow(WaypointRow row, Waypoint w, Function<Waypoint, Component> inlineProvider)
    {
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

    public boolean isCollapsed() { return collapsed; }

    // Flip collapsed state WITHOUT persisting (no onCollapseChange fired). Used by the
    // spring-loaded auto-expand during a drag; persistence happens via confirmTransientExpand()
    // when a drop confirms the expansion.
    public void setExpandedTransient(boolean expanded)
    {
        if (collapsed == !expanded) return;
        collapsed = !expanded;
        body.setVisible(expanded);
        headerLabel.setText(headerText());
        chevron.setIcon(chevronIcon());
        revalidate();
        repaint();
    }

    // Promote a transient expansion to persistent: fire onCollapseChange(false) once so the
    // panel's persisted collapse map reflects "user wants this open". No-op if collapsed.
    public void confirmTransientExpand()
    {
        if (!collapsed) onCollapseChange.accept(false);
    }
}
