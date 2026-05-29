package com.waypointer.ui;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;

// Two-tab strip painted across the top of TabHost. Active tab gets a BRAND_ORANGE
// 2-px matte border on the bottom edge; inactive gets a DARKER_GRAY_COLOR matte of
// the same thickness so the row's height stays constant on swap. Custom-rolled
// instead of JTabbedPane to keep full control over the styling and avoid LAF
// surprises (the panel is constructed before RuneLiteLAF is installed).
final class TabStrip extends JPanel
{
    enum Tab { MY_WAYPOINTS, PRESETS, DEV }

    private final Map<Tab, JLabel> labels = new EnumMap<>(Tab.class);
    private final Consumer<Tab> onSelect;
    private Tab active = Tab.MY_WAYPOINTS;

    TabStrip(Consumer<Tab> onSelect)
    {
        this(onSelect, java.util.Arrays.asList(Tab.MY_WAYPOINTS, Tab.PRESETS));
    }

    TabStrip(Consumer<Tab> onSelect, java.util.List<Tab> visible)
    {
        this.onSelect = onSelect;
        setLayout(new GridLayout(1, visible.size(), 0, 0));
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        for (Tab t : visible)
        {
            labels.put(t, buildTab(displayName(t), t));
        }

        for (JLabel l : labels.values())
        {
            add(l);
        }
        applyStyles();
    }

    private static String displayName(Tab t)
    {
        switch (t)
        {
            case MY_WAYPOINTS: return "My waypoints";
            case PRESETS:      return "Presets";
            case DEV:          return "Dev";
        }
        return t.name();
    }

    Tab getActive() { return active; }

    void setActive(Tab tab)
    {
        if (tab == active) return;
        active = tab;
        applyStyles();
    }

    // Test seam - lets TabStripTest grab a specific tab label to dispatch mouse events.
    JLabel labelFor(Tab tab) { return labels.get(tab); }

    private JLabel buildTab(String text, Tab tab)
    {
        JLabel l = new JLabel(text, SwingConstants.CENTER);
        l.setFont(FontManager.getRunescapeBoldFont());
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        l.setOpaque(true);
        l.setBackground(ColorScheme.DARK_GRAY_COLOR);
        l.addMouseListener(new MouseAdapter()
        {
            @Override public void mouseClicked(MouseEvent e)
            {
                onSelect.accept(tab);
            }
        });
        return l;
    }

    private void applyStyles()
    {
        for (Map.Entry<Tab, JLabel> e : labels.entrySet())
        {
            JLabel l = e.getValue();
            boolean isActive = e.getKey() == active;
            l.setForeground(isActive ? Color.WHITE : ColorScheme.LIGHT_GRAY_COLOR);
            Color underline = isActive
                ? ColorScheme.BRAND_ORANGE
                : ColorScheme.DARKER_GRAY_COLOR;
            l.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 2, 0, underline),
                BorderFactory.createEmptyBorder(8, 0, 8, 0)));
        }
    }
}
