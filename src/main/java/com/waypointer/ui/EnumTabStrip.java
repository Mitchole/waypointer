package com.waypointer.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;

// Generic tab strip: a horizontal row of clickable labels, one per enum constant supplied in
// `visible`. The active tab gets a BRAND_ORANGE matte underline; inactive tabs get a
// DARKER_GRAY_COLOR matte of equal thickness so the row height stays constant on swap.
// Custom-rolled instead of JTabbedPane to keep full styling control and avoid LAF surprises
// (these strips are constructed before RuneLiteLAF is installed).
abstract class EnumTabStrip<E extends Enum<E>> extends JPanel
{
    private final Map<E, JLabel> labels;
    private final Consumer<E> onSelect;
    private final Font font;
    private final int underlineThickness;
    private final Insets labelInsets;
    private E active;

    EnumTabStrip(Class<E> type, List<E> visible, E initialActive, Consumer<E> onSelect,
        Function<E, String> displayName, Font font, int underlineThickness, Insets labelInsets)
    {
        this.labels = new EnumMap<>(type);
        this.onSelect = onSelect;
        this.font = font;
        this.underlineThickness = underlineThickness;
        this.labelInsets = labelInsets;
        this.active = initialActive;

        setLayout(new GridLayout(1, visible.size(), 0, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);
        for (E e : visible)
        {
            labels.put(e, buildTab(displayName.apply(e), e));
        }
        for (JLabel l : labels.values())
        {
            add(l);
        }
        applyStyles();
    }

    final E getActive() { return active; }

    final void setActive(E tab)
    {
        if (tab == active) return;
        active = tab;
        applyStyles();
    }

    // Test seam - lets strip tests grab a specific tab label to dispatch mouse events.
    final JLabel labelFor(E tab) { return labels.get(tab); }

    private JLabel buildTab(String text, E tab)
    {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(font);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setOpaque(true);
        l.setBackground(ColorScheme.DARK_GRAY_COLOR);
        l.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e) { onSelect.accept(tab); }
        });
        return l;
    }

    private void applyStyles()
    {
        for (Map.Entry<E, JLabel> e : labels.entrySet())
        {
            JLabel l = e.getValue();
            boolean isActive = e.getKey() == active;
            l.setForeground(isActive ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
            Color underline = isActive ? ColorScheme.BRAND_ORANGE : ColorScheme.DARKER_GRAY_COLOR;
            l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, underlineThickness, 0, underline),
                BorderFactory.createEmptyBorder(labelInsets.top, labelInsets.left,
                    labelInsets.bottom, labelInsets.right)));
        }
    }
}
