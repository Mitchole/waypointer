package com.waypointer.ui;

import com.waypointer.model.Library;
import com.waypointer.model.Waypoint;
import com.waypointer.model.WorldPointPacker;
import com.waypointer.preset.Preset;
import com.waypointer.preset.PresetCatalog;
import com.waypointer.preset.PresetImport;
import com.waypointer.preset.PresetWaypoint;
import com.waypointer.service.WaypointStore;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import net.runelite.client.game.SpriteManager;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

// The preset browser. Lists curated sets as collapsible sections. Adding or removing a row's
// waypoint flips that row's toggle in place without rebuilding the panel.
class PresetBrowserPanel extends PluginPanel
{
    private final PresetCatalog catalog;
    private final WaypointStore store;
    private final SpriteManager spriteManager;
    private final JPanel body = new JPanel();
    private final ToastBar toast = new ToastBar();

    PresetBrowserPanel(PresetCatalog catalog, WaypointStore store, SpriteManager spriteManager,
        Runnable onBack)
    {
        super(false);
        this.catalog = catalog;
        this.store = store;
        this.spriteManager = spriteManager;

        setLayout(new BorderLayout());
        setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel northStack = new JPanel();
        northStack.setLayout(new BoxLayout(northStack, BoxLayout.Y_AXIS));
        northStack.setBackground(ColorScheme.DARK_GRAY_COLOR);
        JPanel header = buildHeader(onBack);
        header.setAlignmentX(LEFT_ALIGNMENT);
        northStack.add(header);
        northStack.add(toast);
        add(northStack, BorderLayout.NORTH);

        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(ColorScheme.DARK_GRAY_COLOR);

        JPanel bodyHolder = new JPanel(new BorderLayout());
        bodyHolder.setBackground(ColorScheme.DARK_GRAY_COLOR);
        bodyHolder.add(body, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(bodyHolder);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getViewport().setBackground(ColorScheme.DARK_GRAY_COLOR);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        add(scroll, BorderLayout.CENTER);

        rebuild();
    }

    // PluginPanel feeds its layout-computed height to ClientUI; the inner JScrollPane handles
    // overflow, so the height reported outward stays 0 and the frame size keeps tracking the
    // game canvas. Mirrors WaypointerPanel.
    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(super.getPreferredSize().width, 0);
    }

    @Override
    public Dimension getMinimumSize()
    {
        return new Dimension(super.getMinimumSize().width, 0);
    }

    private JPanel buildHeader(Runnable onBack)
    {
        JPanel header = new JPanel(new BorderLayout(8, 0));
        header.setBackground(ColorScheme.DARK_GRAY_COLOR);
        header.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // U+2190 leftwards arrow.
        JLabel back = new JLabel("← Back");
        back.setForeground(Color.LIGHT_GRAY);
        back.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        back.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                onBack.run();
            }
        });
        header.add(back, BorderLayout.WEST);

        JLabel title = new JLabel("Preset waypoints");
        title.setForeground(Color.WHITE);
        title.setFont(FontManager.getRunescapeBoldFont());
        header.add(title, BorderLayout.EAST);

        return header;
    }

    private void rebuild()
    {
        body.removeAll();
        Map<Integer, UUID> existing = existingIdByPacked();
        List<Preset> presets = catalog.getPresets();
        if (presets.isEmpty())
        {
            JLabel none = new JLabel("<html><div style='text-align:center;padding:24px 12px;"
                + "color:#9b9b9b;'>No presets available.</div></html>");
            none.setAlignmentX(LEFT_ALIGNMENT);
            none.setForeground(Color.LIGHT_GRAY);
            body.add(none);
        }
        else
        {
            for (Preset preset : presets)
            {
                PresetSection section = new PresetSection(
                    preset, spriteManager, existing,
                    wp -> addWaypoint(preset, wp),
                    id -> removeWaypoint(id));
                section.setAlignmentX(LEFT_ALIGNMENT);
                body.add(section);
            }
        }
        body.add(Box.createVerticalGlue());
        body.revalidate();
        body.repaint();
    }

    private Map<Integer, UUID> existingIdByPacked()
    {
        Map<Integer, UUID> map = new HashMap<>();
        for (Waypoint w : store.getLibrary().getWaypoints())
        {
            map.put(w.getPackedWorldPoint(), w.getId());
        }
        return map;
    }

    UUID addWaypoint(Preset preset, PresetWaypoint wp)
    {
        store.importMerge(PresetImport.singleEntryLibrary(preset, wp));
        int packed = WorldPointPacker.pack(wp.getX(), wp.getY(), wp.getPlane());
        return findIdByPacked(store.getLibrary(), packed);
    }

    void removeWaypoint(UUID id)
    {
        Waypoint w = findWaypointById(store.getLibrary(), id);
        String name = w != null ? w.getName() : "waypoint";
        store.deleteWaypoint(id);
        toast.show("Removed " + name);
    }

    private static UUID findIdByPacked(Library library, int packed)
    {
        for (Waypoint w : library.getWaypoints())
        {
            if (w.getPackedWorldPoint() == packed) return w.getId();
        }
        return null;
    }

    private static Waypoint findWaypointById(Library library, UUID id)
    {
        for (Waypoint w : library.getWaypoints())
        {
            if (w.getId().equals(id)) return w;
        }
        return null;
    }

    ToastBar getToastForTest()
    {
        return toast;
    }
}
